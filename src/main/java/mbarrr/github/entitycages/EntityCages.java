package mbarrr.github.entitycages;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class EntityCages extends JavaPlugin implements Listener {

    private ItemStack cageItem = new ItemStack(Material.STONE_AXE);

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public ItemStack getCageItem(){
        return cageItem;
    }

    @EventHandler
    void playerClickAnimalEvent(PlayerInteractEntityEvent e){
        ItemStack clickedItem = e.getPlayer().getInventory().getItemInMainHand();

        //Stop the function if the item is not the cage item.
        if(!clickedItem.isSimilar(cageItem)) return;
        //Stop the function if the entity is not an animal.
        if(!(e.getRightClicked() instanceof Animals)) return;

        //Get persistent data from item
        NamespacedKey key = new NamespacedKey(this, "trapped-animal");
        ItemMeta itemMeta = clickedItem.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        //Stop the function if an animal is already stored in the item
        if(container.has(key , PersistentDataType.STRING)) return;

        //get the animal that was clicked
        Animals animal = (Animals) e.getRightClicked();

        //get the animal UUID and store it in the item
        UUID uuid = animal.getUniqueId();
        String stringID = uuid.toString();
        container.set(key, PersistentDataType.STRING, stringID);

        //set the animal to invulnerable, 0 gravity and teleport it way up in the air, creating the illusion the animal has disappeared, also disable the animal's AI
        animal.setInvulnerable(true);
        animal.setGravity(false);
        animal.setPersistent(true);
        animal.setAI(false);
        animal.teleport(new Location(animal.getWorld(), 0, 1000, 0));

        clickedItem.setItemMeta(itemMeta);
    }

    @EventHandler
    void playerClickEvent(PlayerInteractEvent e){
        Bukkit.broadcastMessage("Clickign");
        ItemStack clickedItem = e.getPlayer().getInventory().getItemInMainHand();

        //Stop the function if the item is not the cage item.
        if(!clickedItem.getType().equals(cageItem.getType())) {
            Bukkit.broadcastMessage("item not similar");
            return;
        }

        if(e.getClickedBlock() == null) return;

        //Get persistent data from item
        NamespacedKey key = new NamespacedKey(this, "trapped-animal");
        ItemMeta itemMeta = clickedItem.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        //Stop the function if there is no animal in the cage
        if(!container.has(key , PersistentDataType.STRING)) {
            Bukkit.broadcastMessage("data not found");
            return;
        }

        String stringID = container.get(key, PersistentDataType.STRING);
        UUID uuid = UUID.fromString(stringID);
        Animals animal = (Animals) Bukkit.getEntity(uuid);

        animal.setInvulnerable(false);
        animal.setGravity(true);
        animal.setPersistent(false);
        animal.setAI(true);

        animal.teleport(e.getClickedBlock().getRelative(e.getBlockFace()).getLocation());

        container.remove(key);
        clickedItem.setItemMeta(itemMeta);
        Bukkit.broadcastMessage("done");


    }
}
