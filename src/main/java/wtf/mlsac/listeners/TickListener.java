package wtf.mlsac.listeners;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;

import wtf.mlsac.checks.AICheck;
import wtf.mlsac.session.ISessionManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@SuppressWarnings("unused")
public class TickListener implements Listener {
    
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;
    private HitListener hitListener;
    private int currentTick;
    
    public TickListener(ISessionManager sessionManager, AICheck aiCheck) {
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
        this.currentTick = 0;
    }
    
    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }
    
    @EventHandler
    public void onServerTick(ServerTickEndEvent event) {
        currentTick++;
        
        if (hitListener != null) {
            hitListener.setCurrentTick(currentTick);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (aiCheck != null) {
                aiCheck.onTick(player);
            }
        }
    }
    
    public int getCurrentTick() {
        return currentTick;
    }
}
