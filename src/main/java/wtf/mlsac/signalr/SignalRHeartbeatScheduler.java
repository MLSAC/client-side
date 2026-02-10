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
import java.util.logging.Logger;

public class SignalRHeartbeatScheduler {
    private static final int DEFAULT_INTERVAL_SECONDS = 30;
    private final JavaPlugin plugin;
    private final SignalRSessionManager sessionManager;
    private final Logger logger;
    private ScheduledTask scheduledTask;
    private Runnable onSessionExpiredCallback;
    private Runnable onConnectionLostCallback;
    private int failureCount = 0;
    private static final int MAX_FAILURES = 2;

    public SignalRHeartbeatScheduler(JavaPlugin plugin, SignalRSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.logger = plugin.getLogger();
    }

    public void start() {
        start(DEFAULT_INTERVAL_SECONDS);
    }

    public void start(int intervalSeconds) {
        if (scheduledTask != null) {
            stop();
        }
        long intervalTicks = intervalSeconds * 20L;
        scheduledTask = SchedulerManager.getAdapter().runAsyncRepeating(this::sendHeartbeat, intervalTicks,
                intervalTicks);
        logger.info("[SignalR] Heartbeat scheduler started (interval: " + intervalSeconds + "s)");
    }

    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
            logger.info("[SignalR] Heartbeat scheduler stopped");
        }
    }

    private void sendHeartbeat() {
        if (!sessionManager.isSessionValid()) {
            // Check if it's because it's disconnected
            if (sessionManager.getConnectionState() != com.microsoft.signalr.HubConnectionState.CONNECTED) {
                handleConnectionLost("Connection state is " + sessionManager.getConnectionState());
            }
            return;
        }
        sessionManager.sendHeartbeat()
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        failureCount = 0;
                    } else {
                        failureCount++;
                        String error = result.getError();
                        logger.warning(
                                "[SignalR] Heartbeat failed (" + failureCount + "/" + MAX_FAILURES + "): " + error);

                        if (error != null && (error.contains("expired") || error.contains("invalid")
                                || error.contains("NOT_AUTHENTICATED"))) {
                            if (onSessionExpiredCallback != null) {
                                onSessionExpiredCallback.run();
                            }
                        } else if (failureCount >= MAX_FAILURES) {
                            handleConnectionLost(error);
                        }
                    }
                }).exceptionally(ex -> {
                    failureCount++;
                    logger.warning("[SignalR] Heartbeat encountered exception (" + failureCount + "/" + MAX_FAILURES
                            + "): " + ex.getMessage());
                    if (failureCount >= MAX_FAILURES) {
                        handleConnectionLost(ex.getMessage());
                    }
                    return null;
                });
    }

    private void handleConnectionLost(String reason) {
        logger.severe("[SignalR] Proactive connection check failed: " + reason);
        if (onConnectionLostCallback != null) {
            onConnectionLostCallback.run();
        }
    }

    public void setOnConnectionLostCallback(Runnable callback) {
        this.onConnectionLostCallback = callback;
    }

    public void setOnSessionExpiredCallback(Runnable callback) {
        this.onSessionExpiredCallback = callback;
    }

    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }
}