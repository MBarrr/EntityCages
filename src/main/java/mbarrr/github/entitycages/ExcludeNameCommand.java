package mbarrr.github.entitycages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class ExcludeNameCommand implements CommandExecutor {

    EntityCages instance;

    public ExcludeNameCommand(EntityCages instance){
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name;

        if(!(sender instanceof Player)) return false;
        if(args.length != 2 && args.length != 0) return false;

        Player player = (Player) sender;


        if(args.length == 0) {
            instance.sendPlayerMessage(player, "The following entity names are excluded: " +instance.getExcludedNames());
            return true;
        }

        name = args[0];
        boolean bool = Boolean.parseBoolean(args[1]);

        //add
        if(bool){
            //successfully added
            if(instance.addNameExclusion(name)){
                instance.sendPlayerMessage(player,"Name successfully excluded.");
            }
            //entity already there
            else{
                instance.sendPlayerMessage(player, "Name already excluded");
            }
        }

        //remove
        else{
            //successfully removed
            if(instance.removeNameExclusion(name)){
                instance.sendPlayerMessage(player, "Name successfully removed from exclusions");
            }
            //entity not there
            else{
                instance.sendPlayerMessage(player, "Name is not in exclusions");
            }
        }

        return true;
    }
}
