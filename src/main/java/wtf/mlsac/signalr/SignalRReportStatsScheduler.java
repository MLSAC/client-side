package wtf.mlsac.signalr;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.logging.Logger;

public class SignalRReportStatsScheduler {
    
    private final JavaPlugin plugin;
    private final SignalRSessionManager sessionManager;
    private final IntSupplier onlinePlayersSupplier;
    private final Logger logger;
    
    private BukkitTask scheduledTask;
    private volatile boolean limitExceeded = false;
    private volatile int maxOnline = 0;
    
    private Runnable onLimitExceededCallback;
    private Runnable onLimitClearedCallback;
    private Runnable onSessionExpiredCallback;
    
    public SignalRReportStatsScheduler(JavaPlugin plugin, SignalRSessionManager sessionManager,
                                       IntSupplier onlinePlayersSupplier) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.onlinePlayersSupplier = onlinePlayersSupplier;
        this.logger = plugin.getLogger();
    }
    
    public void start(int intervalSeconds) {
        if (scheduledTask != null) {
            stop();
        }
        
        long intervalTicks = intervalSeconds * 20L;
        
        scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            reportNow();
        }, intervalTicks, intervalTicks);
        
        logger.info("[SignalR] ReportStats scheduler started (interval: " + intervalSeconds + "s)");
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::reportNow);
    }
    
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
            logger.info("[SignalR] ReportStats scheduler stopped");
        }
    }

    public CompletableFuture<SignalRSessionManager.ReportStatsResult> reportNow() {
        int onlinePlayers = onlinePlayersSupplier.getAsInt();
        
        return sessionManager.reportStats(onlinePlayers)
            .thenApply(result -> {
                if (result.isSuccess()) {
                    boolean wasLimitExceeded = this.limitExceeded;
                    this.limitExceeded = result.isLimitExceeded();
                    this.maxOnline = result.getMaxOnline();
                    
                    if (!wasLimitExceeded && this.limitExceeded) {
                        logger.warning("[SignalR] Online limit exceeded (" + onlinePlayers + 
                            "/" + maxOnline + ") - Predict blocked");
                        if (onLimitExceededCallback != null) {
                            onLimitExceededCallback.run();
                        }
                    } else if (wasLimitExceeded && !this.limitExceeded) {
                        logger.info("[SignalR] Online limit cleared - Predict enabled");
                        if (onLimitClearedCallback != null) {
                            onLimitClearedCallback.run();
                        }
                    }
                } else {
                    String error = result.getError();
                    logger.warning("[SignalR] ReportStats failed: " + error);
                    
                    if (error != null && error.contains("NOT_AUTHENTICATED")) {
                        logger.info("[SignalR] Session expired, triggering re-authentication...");
                        if (onSessionExpiredCallback != null) {
                            onSessionExpiredCallback.run();
                        }
                    }
                }
                
                return result;
            });
    }
    
    public boolean isLimitExceeded() {
        return limitExceeded;
    }
    
    public void setLimitExceeded(boolean exceeded) {
        boolean wasLimitExceeded = this.limitExceeded;
        this.limitExceeded = exceeded;
        
        if (!wasLimitExceeded && exceeded && onLimitExceededCallback != null) {
            onLimitExceededCallback.run();
        } else if (wasLimitExceeded && !exceeded && onLimitClearedCallback != null) {
            onLimitClearedCallback.run();
        }
    }
    
    public int getMaxOnline() {
        return maxOnline;
    }
    
    public void setOnLimitExceededCallback(Runnable callback) {
        this.onLimitExceededCallback = callback;
    }
    
    public void setOnLimitClearedCallback(Runnable callback) {
        this.onLimitClearedCallback = callback;
    }
    
    public void setOnSessionExpiredCallback(Runnable callback) {
        this.onSessionExpiredCallback = callback;
    }
    
    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }
}
