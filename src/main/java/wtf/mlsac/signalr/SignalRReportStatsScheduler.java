/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file contains code derived from:
 *   - SlothAC (© 2025 KaelusMC, https://github.com/KaelusMC/SlothAC)
 *   - Grim (© 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim)
 * All derived code is licensed under GPL-3.0.
 */


package wtf.mlsac.signalr;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.mlsac.scheduler.ScheduledTask;
import wtf.mlsac.scheduler.SchedulerManager;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;
import java.util.logging.Logger;
public class SignalRReportStatsScheduler {
    private final JavaPlugin plugin;
    private final SignalRSessionManager sessionManager;
    private final IntSupplier onlinePlayersSupplier;
    private final Logger logger;
    private ScheduledTask scheduledTask;
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
        scheduledTask = SchedulerManager.getAdapter().runAsyncRepeating(() -> {
            reportNow();
        }, intervalTicks, intervalTicks);
        logger.info("[SignalR] ReportStats scheduler started (interval: " + intervalSeconds + "s)");
        SchedulerManager.getAdapter().runAsyncDelayed(this::reportNow, 20L);
    }
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
            logger.info("[SignalR] ReportStats scheduler stopped");
        }
    }
    public CompletableFuture<SignalRSessionManager.ReportStatsResult> reportNow() {
        if (!sessionManager.isSessionValid()) {
            return CompletableFuture.completedFuture(
                new SignalRSessionManager.ReportStatsResult(false, false, 0, "No active session"));
        }
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
                    if (error != null && error.contains("NOT_AUTHENTICATED")) {
                        logger.warning("[SignalR] ReportStats failed: " + error);
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