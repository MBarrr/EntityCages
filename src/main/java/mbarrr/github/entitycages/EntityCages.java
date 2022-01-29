package mbarrr.github.entitycages;

//TODO
//Maybe add some particles
//add different languages
// world protect??

import org.bukkit.*;
import org.bukkit.entity.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EntityCages extends JavaPlugin implements Listener {

    private ItemStack cage;
    private String prefix = "§3[Cages] ";
    private final Material oldCageItemType = Material.GLASS_BOTTLE;
    private final Material cageItemType = Material.HEART_OF_THE_SEA;
    private final String cageKeyString = "cage";
    private final String emptyLoreLine = "§eRight click an animal to capture it.";
    private final String capturedLoreLine = "§eRight click a block to release this animal";
    private List<EntityType> excludedEntities = new ArrayList<>();
    private List<String> excludedNames = new ArrayList<>();
    private boolean allowCageCrafting;
    private boolean allowAllGetCageCommand;
    NamespacedKey cageKey = new NamespacedKey(this, getCageKeyString());
    NamespacedKey key = new NamespacedKey(this, "trapped-animal");
    NamespacedKey worldKey = new NamespacedKey(this, "trapped-animal-world");

    //Config variables
    private boolean allowCaptureTamedAnimals;

    @Override
    public void onEnable() {

        // Plugin startup logic
        this.getCommand("GetCage").setExecutor(new GetCageCommand());
        this.getCommand("excludeEntity").setExecutor(new ExcludeEntityCommand(this));
        this.getCommand("excludeName").setExecutor(new ExcludeNameCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
        cage = makeCage();
        loadDefaultConfig();
        loadConfigVariables();
        loadRecipe();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        List<String> list = new ArrayList<>();
        for(EntityType entityType : excludedEntities){
            list.add(entityType.toString());
        }
        getConfig().set("excludedEntities", list);
        getConfig().set("excludedNames", excludedNames);
        saveConfig();
    }

    //CONFIG METHODS
    public void loadDefaultConfig(){
        if(!getConfig().contains("allowCaptureTamedAnimals")) getConfig().set("allowCaptureTamedAnimals", false);
        if(!getConfig().contains("allowCageCrafting")) getConfig().set("allowCageCrafting", true);
        if(!getConfig().contains("allowAllGetCageCommand")) getConfig().set("allowAllGetCageCommand", false);
        if(!getConfig().contains("excludedEntities")) getConfig().set("excludedEntities", new ArrayList<>());
        if(!getConfig().contains("excludedNames")) getConfig().set("excludedNames", new ArrayList<>());
        saveConfig();
    }

    public void loadConfigVariables(){
        try {
            allowCaptureTamedAnimals = getConfig().getBoolean("allowCaptureTamedAnimals");
            allowCageCrafting = getConfig().getBoolean("allowCageCrafting");
            allowAllGetCageCommand = getConfig().getBoolean("allowAllGetCageCommand");
            excludedNames = getConfig().getStringList("excludedNames");

            List<String> strList = getConfig().getStringList("excludedEntities");
            for(String str : strList){
                excludedEntities.add(EntityType.valueOf(str));
            }
        }catch(Exception e){
            Bukkit.getConsoleSender().sendMessage("An error has occurred while loading startup variables, please check the config for typos/errors.");
        }
    }

    //ITEM METHODS
    private void loadRecipe(){
        if(!allowCageCrafting) return;

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
        if(!(clickedItem.getType().equals(cageItemType) || clickedItem.getType().equals(oldCageItemType))) return;
        //Stop the function if the entity is not an animal.
        if(!(e.getRightClicked() instanceof LivingEntity)) return;

        Player player = e.getPlayer();

        //Get persistent data from item
        ItemMeta itemMeta = clickedItem.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        //Stop the function if an animal is already stored in the item or if the item is not a legit cage
        if(!container.has(cageKey, PersistentDataType.STRING)) return;
        if(container.has(key , PersistentDataType.STRING)) return;

        if(clickedItem.getType().equals(oldCageItemType)) clickedItem.setType(cageItemType);

        //get the animal that was clicked
        LivingEntity animal = (LivingEntity) e.getRightClicked();

        if(excludedEntities.contains(animal.getType())) return;
        if(excludedNames.contains(animal.getName())) return;

        //check if the animal is tameable, if it is, and it has an owner that isnt the player, do not allow capturing
        if(animal instanceof Tameable){
            Tameable tameable = (Tameable) animal;
            if(tameable.getOwner() != null && !tameable.getOwner().equals(player) && !allowCaptureTamedAnimals){
                sendPlayerMessage(player,"You cannot catch this animal, as it belongs to someone else.");
                return;
            }
        }

        //get the animal UUID and store it in the item
        String stringID = animal.getUniqueId().toString();
        container.set(key, PersistentDataType.STRING, stringID);

        //Get the animal's world and store it in the item
        String stringWorld = animal.getWorld().getUID().toString();
        container.set(worldKey, PersistentDataType.STRING, stringWorld);

        //Set lore and set item meta
        setLore(itemMeta, animal, player);
        clickedItem.setItemMeta(itemMeta);

        //set the animal to invulnerable, 0 gravity and teleport it way up in the air, creating the illusion the animal has disappeared, also disable the animal's AI
        animal.setPersistent(true);
        teleportAnimal(animal, false, new Location(animal.getWorld(), 0, 1000, 0));
        animal.setRemoveWhenFarAway(false);
        e.setCancelled(true);
    }

    private void setLore(ItemMeta itemMeta, LivingEntity animal, Player player){
        List<String> lore = new ArrayList<>();
        lore.add(capturedLoreLine);
        lore.add("§5Name: "+animal.getName());

        String type = animal.getType().toString().toLowerCase();
        type = type.substring(0 ,1).toUpperCase() + type.substring(1);

        lore.add("§5Entity Type: "+ type);
        lore.add("§5Captured By: " +player.getName());
        lore.add("§5Health: "+animal.getHealth());
        lore.add("§5UUID: "+animal.getUniqueId());
        itemMeta.setLore(lore);
    }


    @EventHandler
    void playerClickEvent(PlayerInteractEvent e){
        ItemStack clickedItem = e.getPlayer().getInventory().getItemInMainHand();

        //Stop the function if the item is not the cage item.
        if(!(clickedItem.getType().equals(cageItemType) || clickedItem.getType().equals(oldCageItemType))) return;
        if(e.getClickedBlock() == null) return;

        //Get persistent data from item
        ItemMeta itemMeta = clickedItem.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();

        //Stop the function if there is no animal in the cage or if the cage is not a legitimate cage
        if(!container.has(cageKey, PersistentDataType.STRING)) return;
        if(!container.has(key , PersistentDataType.STRING)) return;

        if(clickedItem.getType().equals(oldCageItemType)) clickedItem.setType(cageItemType);

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
            sendPlayerMessage(e.getPlayer(), "An error has occurred, please report this on the plugin page.");
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
        e.setCancelled(true);
    }

    public void sendPlayerMessage(Player player, String message){
        player.sendMessage(prefix+"§a"+message);
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

    public NamespacedKey getCageKey(){
        return cageKey;
    }

    public Material getCageItem(){
        return cageItemType;
    }

    public String getEmptyLoreLine(){
        return emptyLoreLine;
    }

    public boolean getAllowAllGetCageCommand(){
        return allowAllGetCageCommand;
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

    public List<EntityType> getExcludedEntities(){
        return excludedEntities;
    }

    public boolean addEntityExclusion(EntityType entityType){
        if(excludedEntities.contains(entityType)) return false;
        else{
            excludedEntities.add(entityType);
            return true;
        }
    }

    public boolean removeEntityExclusion(EntityType entityType){
        if(!excludedEntities.contains(entityType)) return false;
        else{
            excludedEntities.remove(entityType);
            return true;
        }
    }

    public List<String> getExcludedNames(){
        return excludedNames;
    }

    public boolean addNameExclusion(String name){
        if(excludedNames.contains(name)) return false;
        else{
            excludedNames.add(name);
            return true;
        }
    }

    public boolean removeNameExclusion(String name){
        if(!excludedNames.contains(name)) return false;
        else{
            excludedNames.remove(name);
            return true;
        }
    }
}
