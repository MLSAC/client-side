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
        scheduledTask = SchedulerManager.getAdapter().runAsyncRepeating(this::sendHeartbeat, intervalTicks, intervalTicks);
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
            return;
        }
        sessionManager.sendHeartbeat()
            .thenAccept(result -> {
                if (!result.isSuccess()) {
                    String error = result.getError();
                    if (error != null && (error.contains("expired") || error.contains("invalid"))) {
                        logger.warning("[SignalR] Heartbeat failed: " + error);
                        if (onSessionExpiredCallback != null) {
                            onSessionExpiredCallback.run();
                        }
                    }
                }
            });
    }
    public void setOnSessionExpiredCallback(Runnable callback) {
        this.onSessionExpiredCallback = callback;
    }
    public boolean isRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }
}