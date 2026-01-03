package wtf.mlsac.penalty.handlers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import wtf.mlsac.penalty.ActionHandler;
import wtf.mlsac.penalty.ActionType;
import wtf.mlsac.penalty.PenaltyContext;

public class KickHandler implements ActionHandler {
    
    private final JavaPlugin plugin;
    
    public KickHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void handle(String command, PenaltyContext context) {
        if (command == null || command.isEmpty()) {
            return;
        }
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
    }
    
    @Override
    public ActionType getActionType() {
        return ActionType.KICK;
    }
}
