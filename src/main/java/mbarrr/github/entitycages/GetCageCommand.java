package mbarrr.github.entitycages;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GetCageCommand implements CommandExecutor {


    ItemStack cage = makeCage();



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!(sender instanceof Player)) return false;

        Player player = (Player) sender;

        player.getInventory().addItem(cage);



        return true;
    }

    private ItemStack makeCage(){
        ItemStack cage = new ItemStack(EntityCages.getInstance().getCageItem());
        List<String> lore = new ArrayList<>();
        lore.add(EntityCages.getInstance().getEmptyLoreLine());
        ItemMeta itemMeta = cage.getItemMeta();
        itemMeta.setDisplayName("§eCage");
        itemMeta.setLore(lore);
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(EntityCages.getInstance().getCageKey(), PersistentDataType.STRING, "");
        cage.setItemMeta(itemMeta);
        return cage;
    }
}
