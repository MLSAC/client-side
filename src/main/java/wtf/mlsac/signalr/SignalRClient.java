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

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import wtf.mlsac.server.AIResponse;
import wtf.mlsac.server.IAIClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignalRClient implements IAIClient {
    
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_INITIAL_DELAY_MS = 5000;
    private static final long RECONNECT_MAX_DELAY_MS = 300000;
    private static final double RECONNECT_BACKOFF_MULTIPLIER = 2.0;
    
    private final JavaPlugin plugin;
    private final String serverAddress;
    private final String apiKey;
    private final int reportStatsIntervalSeconds;
    private final Logger logger;
    private final IntSupplier onlinePlayersSupplier;
    
    private SignalRSessionManager sessionManager;
    private SignalRReportStatsScheduler reportStatsScheduler;
    private SignalRHeartbeatScheduler heartbeatScheduler;
    private SignalREndpointConfig endpointConfig;
    
    private String pluginHash;
    private volatile boolean connected = false;
    private volatile boolean autoReconnectEnabled = true;
    private volatile boolean shuttingDown = false;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicInteger autoReconnectAttempts = new AtomicInteger(0);
    
    public SignalRClient(JavaPlugin plugin, String serverAddress, String apiKey,
                         int reportStatsIntervalSeconds, IntSupplier onlinePlayersSupplier) {
        this.plugin = plugin;
        this.serverAddress = serverAddress;
        this.apiKey = apiKey;
        this.reportStatsIntervalSeconds = reportStatsIntervalSeconds;
        this.logger = plugin.getLogger();
        this.onlinePlayersSupplier = onlinePlayersSupplier;
    }

    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.pluginHash = PluginHashCalculator.calculatePluginHash(plugin);
                if (pluginHash.isEmpty()) {
                    logger.warning("[SignalR] Plugin hash calculation failed, using empty hash");
                }
                
                SignalREndpointConfigLoader configLoader = new SignalREndpointConfigLoader(logger);
                try {
                    this.endpointConfig = configLoader.loadSync(serverAddress);
                } catch (Exception e) {
                    logger.warning("[SignalR] Failed to load endpoint config, using defaults: " + e.getMessage());
                    this.endpointConfig = SignalREndpointConfig.defaults();
                }
                
                this.sessionManager = new SignalRSessionManager(serverAddress, endpointConfig, logger);
                sessionManager.initialize();
                
                sessionManager.setOnDisconnectedCallback(this::handleDisconnection);
                
                sessionManager.startConnection().join();
                
                String sessionId = sessionManager.createSession(apiKey, pluginHash).join();
                
                if (sessionId == null || sessionId.isEmpty()) {
                    throw new RuntimeException("Failed to create session");
                }
                
                this.reportStatsScheduler = new SignalRReportStatsScheduler(
                    plugin, sessionManager, onlinePlayersSupplier);
                reportStatsScheduler.setOnLimitExceededCallback(() -> 
                    logger.warning("[SignalR] Online limit exceeded - Predict blocked"));
                reportStatsScheduler.setOnLimitClearedCallback(() -> 
                    logger.info("[SignalR] Online limit cleared - Predict enabled"));
                reportStatsScheduler.setOnSessionExpiredCallback(this::handleSessionExpired);
                reportStatsScheduler.start(reportStatsIntervalSeconds);
                
                this.heartbeatScheduler = new SignalRHeartbeatScheduler(plugin, sessionManager);
                heartbeatScheduler.setOnSessionExpiredCallback(this::handleSessionExpired);
                heartbeatScheduler.start();
                
                connected = true;
                reconnectAttempts.set(0);
                autoReconnectAttempts.set(0);
                logger.info("[SignalR] Connected to " + serverAddress);
                
                return true;
                
            } catch (SignalRSessionManager.AuthenticationException e) {
                logger.severe("[SignalR] Authentication failed: " + e.getMessage());
                connected = false;
                return false;
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[SignalR] Connection failed: " + e.getMessage(), e);
                connected = false;
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> connectWithRetry() {
        return connectWithRetry(0);
    }
    
    private CompletableFuture<Boolean> connectWithRetry(int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            logger.severe("[SignalR] Max retry attempts reached, giving up");
            return CompletableFuture.completedFuture(false);
        }
        
        return connect().thenCompose(success -> {
            if (success) {
                return CompletableFuture.completedFuture(true);
            }
            
            long backoffMs = calculateBackoff(attempt);
            logger.info("[SignalR] Retrying connection in " + backoffMs + "ms (attempt " + 
                (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + ")");
            
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                connectWithRetry(attempt + 1).thenAccept(future::complete);
            }, backoffMs / 50);
            
            return future;
        });
    }
    
    public static long calculateBackoff(int attempt) {
        return INITIAL_BACKOFF_MS * (1L << attempt);
    }
    
    public CompletableFuture<Void> disconnect() {
        shuttingDown = true;
        autoReconnectEnabled = false;
        
        return CompletableFuture.runAsync(() -> {
            try {
                if (heartbeatScheduler != null) {
                    heartbeatScheduler.stop();
                }
                
                if (reportStatsScheduler != null) {
                    reportStatsScheduler.stop();
                }
                
                if (sessionManager != null) {
                    try {
                        sessionManager.closeSession().get(SHUTDOWN_TIMEOUT_SECONDS, 
                            java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.warning("[SignalR] Error closing session: " + e.getMessage());
                    }
                }
                
                connected = false;
                logger.info("[SignalR] Disconnected from server");
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "[SignalR] Error during disconnect", e);
            }
        });
    }

    private void handleDisconnection(Throwable exception) {
        connected = false;
        
        if (shuttingDown || !autoReconnectEnabled) {
            return;
        }
        
        if (exception != null) {
            logger.warning("[SignalR] Connection lost: " + exception.getMessage());
        } else {
            logger.warning("[SignalR] Connection lost unexpectedly");
        }
        
        if (reportStatsScheduler != null) {
            reportStatsScheduler.stop();
        }
        
        if (heartbeatScheduler != null) {
            heartbeatScheduler.stop();
        }
        
        scheduleReconnect();
    }
    
    private void handleSessionExpired() {
        if (shuttingDown || !autoReconnectEnabled) {
            return;
        }
        
        logger.info("[SignalR] Re-authenticating session...");
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String newSessionId = sessionManager.createSession(apiKey, pluginHash).join();
                if (newSessionId != null && !newSessionId.isEmpty()) {
                    logger.info("[SignalR] Session re-authenticated successfully");
                } else {
                    logger.warning("[SignalR] Session re-authentication failed, scheduling reconnect");
                    scheduleReconnect();
                }
            } catch (Exception e) {
                logger.warning("[SignalR] Session re-authentication error: " + e.getMessage());
                scheduleReconnect();
            }
        });
    }
    
    private void scheduleReconnect() {
        int attempt = autoReconnectAttempts.incrementAndGet();
        
        if (attempt > MAX_RECONNECT_ATTEMPTS) {
            logger.severe("[SignalR] Max reconnect attempts (" + MAX_RECONNECT_ATTEMPTS + 
                ") reached, giving up. Manual reconnect required.");
            autoReconnectAttempts.set(0);
            return;
        }
        
        long delayMs = calculateReconnectDelay(attempt);
        long delaySeconds = delayMs / 1000;
        
        logger.info("[SignalR] Scheduling reconnect attempt " + attempt + "/" + MAX_RECONNECT_ATTEMPTS + 
            " in " + delaySeconds + " seconds...");
        
        long delayTicks = delayMs / 50;
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::attemptReconnect, delayTicks);
    }

    private void attemptReconnect() {
        if (shuttingDown || !autoReconnectEnabled) {
            return;
        }
        
        if (connected) {
            logger.info("[SignalR] Already connected, skipping reconnect");
            autoReconnectAttempts.set(0);
            return;
        }
        
        int attempt = autoReconnectAttempts.get();
        logger.info("[SignalR] Attempting reconnect (" + attempt + "/" + MAX_RECONNECT_ATTEMPTS + ")...");
        
        connect().thenAccept(success -> {
            if (success) {
                logger.info("[SignalR] Reconnected successfully after " + attempt + " attempt(s)");
                autoReconnectAttempts.set(0);
            } else {
                logger.warning("[SignalR] Reconnect attempt " + attempt + " failed");
                scheduleReconnect();
            }
        }).exceptionally(ex -> {
            logger.warning("[SignalR] Reconnect attempt " + attempt + " failed: " + ex.getMessage());
            scheduleReconnect();
            return null;
        });
    }
    
    private long calculateReconnectDelay(int attempt) {
        double delay = RECONNECT_INITIAL_DELAY_MS * Math.pow(RECONNECT_BACKOFF_MULTIPLIER, attempt - 1);
        return Math.min((long) delay, RECONNECT_MAX_DELAY_MS);
    }
    
    public void setAutoReconnectEnabled(boolean enabled) {
        this.autoReconnectEnabled = enabled;
    }
    
    public boolean isAutoReconnectEnabled() {
        return autoReconnectEnabled;
    }
    
    public CompletableFuture<Boolean> reconnect() {
        autoReconnectAttempts.set(0);
        
        return disconnect().thenCompose(v -> {
            shuttingDown = false;
            autoReconnectEnabled = true;
            return connect();
        });
    }

    public CompletableFuture<AIResponse> predict(byte[] playerData, String playerUuid) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Not connected to SignalR server"));
        }
        
        if (reportStatsScheduler != null && reportStatsScheduler.isLimitExceeded()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Online limit exceeded, Predict blocked"));
        }
        
        return sessionManager.predict(playerData, playerUuid)
            .thenCompose(result -> {
                if (result.isSuccess()) {
                    return CompletableFuture.completedFuture(
                        new AIResponse(result.getProbability()));
                }
                
                String errorCode = result.getErrorCode();
                
                if (HubErrorParser.requiresReportStats(errorCode)) {
                    logger.warning("[SignalR] Predict failed: " + errorCode + ", calling ReportStats...");
                    return handleStatsRequiredAndRetry(playerData, playerUuid);
                }
                
                if (HubErrorParser.LIMIT_EXCEEDED.equals(errorCode)) {
                    logger.warning("[SignalR] Predict failed: Online limit exceeded");
                    if (reportStatsScheduler != null) {
                        reportStatsScheduler.setLimitExceeded(true);
                    }
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Online limit exceeded"));
                }
                
                if (HubErrorParser.NOT_AUTHENTICATED.equals(errorCode)) {
                    logger.warning("[SignalR] Session expired during prediction, attempting reconnect");
                    return handleUnauthenticatedAndRetry(playerData, playerUuid);
                }
                
                return CompletableFuture.failedFuture(
                    new RuntimeException(errorCode + ": " + result.getErrorMessage()));
            });
    }

    private CompletableFuture<AIResponse> handleStatsRequiredAndRetry(byte[] playerData, String playerUuid) {
        if (reportStatsScheduler == null) {
            return CompletableFuture.failedFuture(
                new RuntimeException("ReportStats scheduler not initialized"));
        }
        
        return reportStatsScheduler.reportNow()
            .thenCompose(statsResult -> {
                if (!statsResult.isSuccess()) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("ReportStats failed: " + statsResult.getError()));
                }
                
                if (statsResult.isLimitExceeded()) {
                    return CompletableFuture.failedFuture(
                        new RuntimeException("Online limit exceeded after ReportStats"));
                }
                
                return sessionManager.predict(playerData, playerUuid)
                    .thenApply(result -> {
                        if (result.isSuccess()) {
                            return new AIResponse(result.getProbability());
                        }
                        throw new RuntimeException(result.getErrorCode() + ": " + result.getErrorMessage());
                    });
            });
    }
    
    private CompletableFuture<AIResponse> handleUnauthenticatedAndRetry(byte[] playerData, String playerUuid) {
        return sessionManager.createSession(apiKey, pluginHash)
            .thenCompose(sessionId -> {
                return sessionManager.predict(playerData, playerUuid)
                    .thenApply(result -> {
                        if (result.isSuccess()) {
                            return new AIResponse(result.getProbability());
                        }
                        throw new RuntimeException(result.getErrorCode() + ": " + result.getErrorMessage());
                    });
            });
    }
    
    @Override
    public boolean isConnected() {
        return connected && sessionManager != null && sessionManager.isSessionValid();
    }
    
    @Override
    public boolean isLimitExceeded() {
        return reportStatsScheduler != null && reportStatsScheduler.isLimitExceeded();
    }
    
    @Override
    public String getSessionId() {
        return sessionManager != null ? sessionManager.getSessionId() : null;
    }
    
    @Override
    public String getServerAddress() {
        return serverAddress;
    }
    
    public SignalREndpointConfig getEndpointConfig() {
        return endpointConfig;
    }
}
