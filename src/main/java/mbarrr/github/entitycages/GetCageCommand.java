package mbarrr.github.entitycages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GetCageCommand implements CommandExecutor {




    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(!(sender instanceof Player)) return false;

        Player player = (Player) sender;



        if(!player.hasPermission("EntityCages.GetCage") && !EntityCages.getInstance().getAllowAllGetCageCommand() && !player.isOp()){
            EntityCages.getInstance().sendPlayerMessage(player, "You do not have permission to do this.");
            return true;
        }

        player.getInventory().addItem(EntityCages.getInstance().getCage());



        return true;
    }
}
