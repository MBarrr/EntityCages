package mbarrr.github.entitycages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class ExcludeEntityCommand implements CommandExecutor {

    private EntityCages instance;


    public ExcludeEntityCommand(EntityCages instance){
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EntityType entityType;

        if(!(sender instanceof Player)) return false;
        if(args.length != 2 && args.length != 0) return false;

        Player player = (Player) sender;


        if(args.length == 0) {
            instance.sendPlayerMessage(player, "The following entity types are excluded: " +instance.getExcludedEntities());
            return true;
        }

        try {
            entityType = EntityType.valueOf(args[0]);
        }catch(IllegalArgumentException e){
            instance.sendPlayerMessage(player, args[0] + " could not be matched to an entity type.");
            return true;
        }

        boolean bool = Boolean.parseBoolean(args[1]);

        //add
        if(bool){
            //successfully added
            if(instance.addEntityExclusion(entityType)){
                instance.sendPlayerMessage(player,"EntityType successfully excluded.");
            }
            //entity already there
            else{
                instance.sendPlayerMessage(player, "EntityType already excluded");
            }
        }

        //remove
        else{
            //successfully removed
            if(instance.removeEntityExclusion(entityType)){
                instance.sendPlayerMessage(player, "EntityType successfully removed from exclusions");
            }
            //entity not there
            else{
                instance.sendPlayerMessage(player, "EntityType is not in exclusions");
            }
        }



        return true;
    }
}
