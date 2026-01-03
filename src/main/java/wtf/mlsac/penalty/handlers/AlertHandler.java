package wtf.mlsac.penalty.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import wtf.mlsac.Permissions;
import wtf.mlsac.penalty.ActionHandler;
import wtf.mlsac.penalty.ActionType;
import wtf.mlsac.penalty.PenaltyContext;
import wtf.mlsac.util.ColorUtil;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class AlertHandler implements ActionHandler {
    
    private final JavaPlugin plugin;
    private final Logger logger;
    private String alertPrefix;
    private Set<UUID> alertRecipients;
    private boolean consoleAlerts;
    
    public AlertHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.alertPrefix = "&6[ALERT] &f";
        this.consoleAlerts = true;
    }
    
    public void setAlertPrefix(String prefix) {
        this.alertPrefix = prefix != null ? prefix : "&6[ALERT] &f";
    }
    
    public void setAlertRecipients(Set<UUID> recipients) {
        this.alertRecipients = recipients;
    }
    
    public void setConsoleAlerts(boolean enabled) {
        this.consoleAlerts = enabled;
    }
    
    @Override
    public void handle(String message, PenaltyContext context) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        String formattedMessage = ColorUtil.colorize(alertPrefix + message);
        
        if (alertRecipients != null) {
            for (UUID uuid : alertRecipients) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && canReceiveAlerts(player)) {
                    player.sendMessage(formattedMessage);
                }
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (canReceiveAlerts(player)) {
                    player.sendMessage(formattedMessage);
                }
            }
        }
        
        if (consoleAlerts) {
            logger.info(ColorUtil.stripColors(formattedMessage));
        }
    }
    
    private boolean canReceiveAlerts(Player player) {
        return player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN);
    }
    
    @Override
    public ActionType getActionType() {
        return ActionType.CUSTOM_ALERT;
    }
}
