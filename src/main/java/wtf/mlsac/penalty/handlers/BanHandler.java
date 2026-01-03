package wtf.mlsac.penalty.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import wtf.mlsac.penalty.ActionHandler;
import wtf.mlsac.penalty.ActionType;
import wtf.mlsac.penalty.BanAnimation;
import wtf.mlsac.penalty.PenaltyContext;

public class BanHandler implements ActionHandler {
    
    private final JavaPlugin plugin;
    private final BanAnimation animation;
    private boolean animationEnabled = true;
    
    public BanHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.animation = new BanAnimation(plugin);
    }
    
    @Override
    public void handle(String command, PenaltyContext context) {
        if (command == null || command.isEmpty()) {
            return;
        }
        
        Player player = null;
        if (context != null && context.getPlayerName() != null) {
            player = Bukkit.getPlayer(context.getPlayerName());
        }
        
        if (animationEnabled && player != null && player.isOnline()) {
            animation.playAnimation(player, command, context);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            });
        }
    }
    
    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }
    
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }
    
    public BanAnimation getAnimation() {
        return animation;
    }
    
    public void shutdown() {
        animation.shutdown();
    }
    
    @Override
    public ActionType getActionType() {
        return ActionType.BAN;
    }
}
