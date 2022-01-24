package mbarrr.github.entitycages;

//TODO
//Maybe add some particles
//make type in description lower case

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EntityCages extends JavaPlugin implements Listener {

    private ItemStack cage;
    private Material cageItemType = Material.GLASS_BOTTLE;
    private String cageKeyString = "cage";
    private String emptyLoreLine = "§eRight click an animal to capture it.";
    private String capturedLoreLine = "§eRight click a block to release this animal";
    NamespacedKey cageKey = new NamespacedKey(this, getCageKeyString());
    NamespacedKey key = new NamespacedKey(this, "trapped-animal");
    NamespacedKey worldKey = new NamespacedKey(this, "trapped-animal-world");

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getCommand("GetCage").setExecutor(new GetCageCommand());
        getServer().getPluginManager().registerEvents(this, this);
        cage = makeCage();
        loadRecipe();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        
    }

    private void loadRecipe(){
        ShapedRecipe cageRecipe = new ShapedRecipe(
                new NamespacedKey(this, "test"),
                cage
        );
        cageRecipe.shape("CCC", "C C", "CCC");
        cageRecipe.setIngredient('C', Material.IRON_BARS);
        Bukkit.addRecipe(cageRecipe);
    }

    private ItemStack makeCage(){
        ItemStack cage = new ItemStack(getCageItem());
        List<String> lore = new ArrayList<>();
        lore.add(getEmptyLoreLine());
        ItemMeta itemMeta = cage.getItemMeta();
        itemMeta.setDisplayName("§eCage");
        itemMeta.setLore(lore);
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(getCageKey(), PersistentDataType.STRING, "");
        cage.setItemMeta(itemMeta);
        return cage;
    }

    @EventHandler
    void playerClickAnimalEvent(PlayerInteractEntityEvent e){
        ItemStack clickedItem = e.getPlayer().getInventory().getItemInMainHand();

        //Stop the function if the item is not the cage item.
        if(!clickedItem.getType().equals(cageItemType)) return;
        //Stop the function if the entity is not an animal.
        if(!(e.getRightClicked() instanceof LivingEntity)) return;

        //Get persistent data from item
        ItemMeta itemMeta = clickedItem.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        //Stop the function if an animal is already stored in the item or if the item is not a legit cage
        if(!container.has(cageKey, PersistentDataType.STRING)) return;
        if(container.has(key , PersistentDataType.STRING)) return;

        //get the animal that was clicked
        LivingEntity animal = (LivingEntity) e.getRightClicked();

        //get the animal UUID and store it in the item
        String stringID = animal.getUniqueId().toString();
        container.set(key, PersistentDataType.STRING, stringID);

        //Get the animal's world and store it in the item
        String stringWorld = animal.getWorld().getUID().toString();
        container.set(worldKey, PersistentDataType.STRING, stringWorld);

        //Set lore and set item meta
        setLore(itemMeta, animal, e.getPlayer());
        clickedItem.setItemMeta(itemMeta);

        //set the animal to invulnerable, 0 gravity and teleport it way up in the air, creating the illusion the animal has disappeared, also disable the animal's AI
        animal.setPersistent(true);
        teleportAnimal(animal, false, new Location(animal.getWorld(), 0, 1000, 0));
        animal.setRemoveWhenFarAway(false);
    }

    private void setLore(ItemMeta itemMeta, LivingEntity animal, Player player){
        List<String> lore = new ArrayList<>();
        lore.add(capturedLoreLine);
        lore.add("§5Name: "+animal.getName());
        lore.add("§5Entity Type: "+ animal.getType());
        lore.add("§5Captured By: " +player.getName());
        lore.add("§5Health: "+animal.getHealth());
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

        //Get world from item data and load the chunk the animal is stored in
        UUID worldID = UUID.fromString(container.get(worldKey, PersistentDataType.STRING));
        World world = Bukkit.getWorld(worldID);

        //Load chunk where entites are stored
        new Location(world, 0, 1000, 0).getChunk().load();

        //get entity and return if null
        LivingEntity animal = (LivingEntity) Bukkit.getEntity(uuid);

        if(animal == null){
            return;
        }

        //remove animal data from item
        container.remove(key);
        container.remove(worldKey);
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

    private void teleportAnimal(LivingEntity animal, boolean bool, Location location){
        animal.setInvulnerable(!bool);
        animal.setGravity(bool);
        animal.setAI(bool);
        animal.setFallDistance(0);
        if(!animal.getPassengers().isEmpty()){
            for(Entity passenger: animal.getPassengers()){
                animal.removePassenger(passenger);
            }
        }
        animal.teleport(location);
    }

    public Material getCageItem(){
        return cageItemType;
    }

    public String getEmptyLoreLine(){
        return emptyLoreLine;
    }

    public ItemStack getCage(){
        return cage;
    }

    public String getCageKeyString(){
        return cageKeyString;
    }

    public static EntityCages getInstance(){
        return (EntityCages) Bukkit.getPluginManager().getPlugin("EntityCages");
    }
}
