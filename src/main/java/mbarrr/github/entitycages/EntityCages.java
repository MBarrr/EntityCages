package mbarrr.github.entitycages;

import com.sun.tools.javac.util.Names;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EntityCages extends JavaPlugin implements Listener {

    private Material cageItemType = Material.GLASS_BOTTLE;
    private String cageKeyString = "cage";
    private String emptyLoreLine = "§eRight click an animal to capture it.";
    private String capturedLoreLine = "§eRight click a block to release this animal";
    NamespacedKey cageKey = new NamespacedKey(this, getCageKeyString());
    NamespacedKey key = new NamespacedKey(this, "trapped-animal");


    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getCommand("GetCage").setExecutor(new GetCageCommand());
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    void playerClickAnimalEvent(PlayerInteractEntityEvent e){
        ItemStack clickedItem = e.getPlayer().getInventory().getItemInMainHand();

        //Stop the function if the item is not the cage item.
        if(!clickedItem.getType().equals(cageItemType)) return;
        //Stop the function if the entity is not an animal.
        if(!(e.getRightClicked() instanceof Animals)) return;

        //Get persistent data from item
        ItemMeta itemMeta = clickedItem.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        //Stop the function if an animal is already stored in the item or if the item is not a legit cage
        if(!container.has(cageKey, PersistentDataType.STRING)) return;
        if(container.has(key , PersistentDataType.STRING)) return;

        //get the animal that was clicked
        Animals animal = (Animals) e.getRightClicked();

        //get the animal UUID and store it in the item
        String stringID = animal.getUniqueId().toString();
        container.set(key, PersistentDataType.STRING, stringID);
        setLore(itemMeta, animal, e.getPlayer());
        clickedItem.setItemMeta(itemMeta);

        //set the animal to invulnerable, 0 gravity and teleport it way up in the air, creating the illusion the animal has disappeared, also disable the animal's AI
        animal.setPersistent(true);
        teleportAnimal(animal, false, new Location(animal.getWorld(), 0, 1000, 0));
        animal.setRemoveWhenFarAway(false);
    }

    private void setLore(ItemMeta itemMeta, Animals animal, Player player){
        List<String> lore = new ArrayList<>();
        lore.add(capturedLoreLine);
        lore.add("§5Name: "+animal.getName());
        lore.add("§5Entity Type: "+ animal.getType());
        lore.add("§5Captured By: " +player.getName());
        lore.add("§5Age: "+animal.getAge());
        lore.add("§5UUID: "+animal.getUniqueId());
        itemMeta.setLore(lore);
    }


    @EventHandler
    void playerClickEvent(PlayerInteractEvent e){
        ItemStack clickedItem = e.getPlayer().getInventory().getItemInMainHand();

        //Stop the function if the item is not the cage item.
        if(!clickedItem.getType().equals(cageItemType)) return;
        if(e.getClickedBlock() == null) return;

        //Get persistent data from item
        ItemMeta itemMeta = clickedItem.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        //Stop the function if there is no animal in the cage or if the cage is not a legitimate cage
        if(!container.has(cageKey, PersistentDataType.STRING)) return;
        if(!container.has(key , PersistentDataType.STRING)) return;

        //Get animal UUID from item data and get the entity
        String stringID = container.get(key, PersistentDataType.STRING);
        UUID uuid = UUID.fromString(stringID);
        Animals animal = (Animals) Bukkit.getEntity(uuid);

        if(animal == null){
            e.getPlayer().sendMessage("An error has occured resulting in your animal not being found. Please let me know about this through my spigot page");
            e.getItem().setAmount(0);
            return;
        }

        //remove animal data from item
        container.remove(key);
        List<String> lore = new ArrayList<>();
        lore.add(emptyLoreLine);
        itemMeta.setLore(lore);
        clickedItem.setItemMeta(itemMeta);

        //teleport animal to safe location near player clicked block
        teleportAnimal(animal, true, e.getClickedBlock().getRelative(e.getBlockFace()).getLocation());
    }


    public NamespacedKey getCageKey(){
        return cageKey;
    }

    private void teleportAnimal(Animals animal, boolean bool, Location location){
        animal.setInvulnerable(!bool);
        animal.setGravity(bool);
        animal.setAI(bool);
        animal.setFallDistance(0);
        animal.teleport(location);
    }

    public Material getCageItem(){
        return cageItemType;
    }

    public String getEmptyLoreLine(){
        return emptyLoreLine;
    }

    public String getCageKeyString(){
        return cageKeyString;
    }

    public static EntityCages getInstance(){
        return (EntityCages) Bukkit.getPluginManager().getPlugin("EntityCages");
    }
}
