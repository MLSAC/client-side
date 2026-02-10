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

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import com.microsoft.signalr.TransportEnum;
import com.microsoft.signalr.messagepack.MessagePackHubProtocol;
import wtf.mlsac.signalr.dto.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignalRSessionManager {
    private static final long SERVER_TIMEOUT_MS = 120000;
    private static final long KEEP_ALIVE_INTERVAL_MS = 30000;
    private final Logger logger;
    private final SignalREndpointConfig endpointConfig;
    private final String hubUrl;
    private final boolean debug;
    private final String pluginVersion;
    private HubConnection hubConnection;
    private volatile String sessionId;
    private volatile boolean sessionValid;
    private volatile long lastServerTime;
    private java.util.function.Consumer<Throwable> onDisconnectedCallback;

    public SignalRSessionManager(String serverAddress, SignalREndpointConfig endpointConfig, Logger logger,
            boolean debug, String pluginVersion) {
        this.logger = logger;
        this.endpointConfig = endpointConfig;
        this.hubUrl = endpointConfig.getHubUrl(serverAddress);
        this.sessionValid = false;
        this.debug = debug;
        this.pluginVersion = pluginVersion;
    }

    public void setOnDisconnectedCallback(java.util.function.Consumer<Throwable> callback) {
        this.onDisconnectedCallback = callback;
    }

    public void initialize() {
        this.hubConnection = HubConnectionBuilder.create(hubUrl)
                .setHttpClientBuilderCallback(builder -> {
                    builder.addInterceptor(new SignalRNegotiateInterceptor(logger, debug));
                })
                .withTransport(TransportEnum.WEBSOCKETS)
                .withHubProtocol(new MessagePackHubProtocol())
                .withServerTimeout(SERVER_TIMEOUT_MS)
                .withKeepAliveInterval(KEEP_ALIVE_INTERVAL_MS)
                .build();
        hubConnection.onClosed(exception -> {
            sessionValid = false;
            if (exception != null) {
                logger.warning("[SignalR] Connection closed with error: " + exception.getMessage());
            } else {
                logger.info("[SignalR] Connection closed");
            }
            if (onDisconnectedCallback != null) {
                onDisconnectedCallback.accept(exception);
            }
        });
    }

    public CompletableFuture<Void> startConnection() {
        if (hubConnection == null) {
            initialize();
        }
        return CompletableFuture.runAsync(() -> {
            try {
                hubConnection.start().blockingAwait();
                logger.info("[SignalR] WebSocket connection established to " + hubUrl);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start SignalR connection: " + e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<String> createSession(String apiKey, String pluginHash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String methodName = endpointConfig.getMethodName("connect");
                if (debug) {
                    logger.info("[SignalR] Calling " + methodName + " with api_key=" +
                            apiKey.substring(0, Math.min(8, apiKey.length())) + "..., plugin_hash=" + pluginHash +
                            ", plugin_version=" + pluginVersion);
                }
                ConnectRequest request = new ConnectRequest(apiKey, pluginHash, pluginVersion);
                ConnectResponse response = hubConnection.invoke(ConnectResponse.class, methodName, request)
                        .blockingGet();
                if (response == null || response.sessionId == null || response.sessionId.isEmpty()) {
                    throw new SessionException("Empty session ID received");
                }
                this.sessionId = response.sessionId;
                this.lastServerTime = response.serverTime;
                this.sessionValid = true;
                logger.info("[SignalR] Session created: " +
                        sessionId.substring(0, Math.min(8, sessionId.length())) + "...");
                return sessionId;
            } catch (Exception e) {
                this.sessionValid = false;
                String errorMsg = e.getMessage();
                HubErrorParser.HubError hubError = HubErrorParser.parse(errorMsg);
                if (HubErrorParser.AUTH_FAILED.equals(hubError.getCode())) {
                    logger.severe("[SignalR] Authentication failed: " + hubError.getMessage());
                    throw new AuthenticationException(hubError.getMessage());
                }
                logger.log(Level.SEVERE, "[SignalR] Session creation failed: " + errorMsg, e);
                throw new SessionException("Session creation failed: " + errorMsg, e);
            }
        });
    }

    public CompletableFuture<HeartbeatResult> sendHeartbeat() {
        if (!isSessionValid()) {
            return CompletableFuture.completedFuture(
                    new HeartbeatResult(false, 0, "No active session"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String methodName = endpointConfig.getMethodName("heartbeat");
                HeartbeatResponse response = hubConnection.invoke(HeartbeatResponse.class, methodName)
                        .blockingGet();
                long serverTime = response != null ? response.serverTime : 0;
                this.lastServerTime = serverTime;
                return new HeartbeatResult(true, serverTime, null);
            } catch (Exception e) {
                HubErrorParser.HubError hubError = HubErrorParser.parse(e.getMessage());
                if (HubErrorParser.NOT_AUTHENTICATED.equals(hubError.getCode())) {
                    this.sessionValid = false;
                    return new HeartbeatResult(false, 0, "Session expired or invalid");
                }
                return new HeartbeatResult(false, 0, "Heartbeat error: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<ReportStatsResult> reportStats(int onlinePlayers) {
        if (!isSessionValid()) {
            if (debug) {
                logger.warning("[SignalR] Cannot call ReportStats - no active session");
            }
            return CompletableFuture.completedFuture(
                    new ReportStatsResult(false, false, 0, "No active session"));
        }

        HubConnectionState state = hubConnection.getConnectionState();
        if (state != HubConnectionState.CONNECTED) {
            if (debug) {
                logger.severe("[SignalR] Cannot call ReportStats - not connected! State: " + state);
            }
            return CompletableFuture.completedFuture(
                    new ReportStatsResult(false, false, 0, "Not connected, state: " + state));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String methodName = endpointConfig.getMethodName("reportStats");
                ReportStatsRequest request = new ReportStatsRequest(onlinePlayers);

                if (debug) {
                    logger.info("[SignalR] Connection state OK, calling ReportStats...");
                    logger.info("[SignalR] Preparing to call " + methodName + " with onlinePlayers=" + onlinePlayers);
                    try {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String json = gson.toJson(request);
                        logger.info("[SignalR] ReportStats request JSON: " + json);
                    } catch (Exception jsonEx) {
                        logger.warning("[SignalR] Failed to serialize request to JSON: " + jsonEx.getMessage());
                    }
                    logger.info("[SignalR] ReportStats invoked, waiting for response...");
                }

                ReportStatsResponse response;
                try {
                    response = hubConnection.invoke(ReportStatsResponse.class, methodName, request)
                            .timeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .toFuture()
                            .get(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException timeoutEx) {
                    if (debug) {
                        logger.severe("[SignalR] ReportStats timeout after 5 seconds");
                    }
                    return new ReportStatsResult(false, false, 0, "Timeout after 5 seconds");
                } catch (java.util.concurrent.ExecutionException execEx) {
                    Throwable cause = execEx.getCause();
                    if (cause != null) {
                        throw cause;
                    }
                    throw execEx;
                }

                if (debug) {
                    logger.info("[SignalR] ReportStats completed successfully!");
                    boolean limitExceeded = response != null && response.limitExceeded;
                    int maxOnline = response != null ? response.maxOnline : 0;
                    logger.info("[SignalR] ReportStats response: limitExceeded=" + limitExceeded +
                            ", maxOnline=" + maxOnline);
                }

                boolean limitExceeded = response != null && response.limitExceeded;
                int maxOnline = response != null ? response.maxOnline : 0;
                return new ReportStatsResult(true, limitExceeded, maxOnline, null);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (debug) {
                    logger.severe("[SignalR] ReportStats interrupted: " + e.getMessage());
                }
                return new ReportStatsResult(false, false, 0, "Interrupted");

            } catch (Throwable e) {
                HubErrorParser.HubError hubError = HubErrorParser.parse(e.getMessage());

                if (HubErrorParser.NOT_AUTHENTICATED.equals(hubError.getCode())) {
                    this.sessionValid = false;
                    if (debug) {
                        logger.warning("[SignalR] ReportStats failed: Session invalidated (NOT_AUTHENTICATED)");
                    }
                    return new ReportStatsResult(false, false, 0, hubError.getMessage());
                }

                if (debug) {
                    logger.severe(
                            "[SignalR] ReportStats failed: " + hubError.getCode() + " - " + hubError.getMessage());
                }

                return new ReportStatsResult(false, false, 0, hubError.getMessage());
            }
        });
    }

    public io.reactivex.rxjava3.core.Observable<PredictResult> predict(byte[] playerData, String playerUuid,
            String playerName) {
        if (!isSessionValid()) {
            return io.reactivex.rxjava3.core.Observable.just(
                    new PredictResult(false, 0, 0, "NOT_AUTHENTICATED", "No active session", null));
        }

        String methodName = "predictStream";
        PredictRequest request = new PredictRequest(playerData, playerUuid, playerName);

        return hubConnection.stream(PredictResponse.class, methodName, request)
                .map(response -> {
                    if (response == null) {
                        return new PredictResult(false, 0, 0, "INVALID_RESPONSE", "Null response from server", null);
                    }
                    if (debug) {
                        logger.info("[SignalR] Predict response received - probability: " + response.probability +
                                ", inferenceTimeMs: " + response.inferenceTimeMs +
                                ", model (raw): '" + response.model + "'");
                    }
                    if (Float.isNaN(response.probability) || Float.isInfinite(response.probability)) {
                        return new PredictResult(false, 0, 0, "INVALID_DATA",
                                "Server returned invalid probability: " + response.probability, null);
                    }
                    float probability = Math.max(0.0f, Math.min(1.0f, response.probability));
                    String modelName = response.model;
                    return new PredictResult(true, probability, response.inferenceTimeMs, null, null, modelName);
                })
                .onErrorReturn(throwable -> {
                    HubErrorParser.HubError hubError = HubErrorParser.parse(throwable.getMessage());
                    String errorCode = hubError.getCode();
                    if (HubErrorParser.NOT_AUTHENTICATED.equals(errorCode)) {
                        this.sessionValid = false;
                    }
                    return new PredictResult(false, 0, 0, errorCode, hubError.getMessage(), null);
                });
    }

    public CompletableFuture<Void> closeSession() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (hubConnection != null) {
                    logger.info("[SignalR] Stopping HubConnection...");
                    hubConnection.stop().blockingAwait();
                    logger.info("[SignalR] HubConnection stopped.");
                }
            } catch (Exception e) {
                logger.warning("[SignalR] Error closing connection: " + e.getMessage());
            } finally {
                this.sessionId = null;
                this.sessionValid = false;
            }
        });
    }

    public boolean isSessionValid() {
        return sessionValid &&
                sessionId != null &&
                hubConnection != null &&
                hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getLastServerTime() {
        return lastServerTime;
    }

    public HubConnectionState getConnectionState() {
        return hubConnection != null ? hubConnection.getConnectionState() : HubConnectionState.DISCONNECTED;
    }

    public void invalidateSession() {
        this.sessionValid = false;
    }

    public static class HeartbeatResult {
        private final boolean success;
        private final long serverTime;
        private final String error;

        public HeartbeatResult(boolean success, long serverTime, String error) {
            this.success = success;
            this.serverTime = serverTime;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getServerTime() {
            return serverTime;
        }

        public String getError() {
            return error;
        }
    }

    public static class ReportStatsResult {
        private final boolean success;
        private final boolean limitExceeded;
        private final int maxOnline;
        private final String error;

        public ReportStatsResult(boolean success, boolean limitExceeded, int maxOnline, String error) {
            this.success = success;
            this.limitExceeded = limitExceeded;
            this.maxOnline = maxOnline;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isLimitExceeded() {
            return limitExceeded;
        }

        public int getMaxOnline() {
            return maxOnline;
        }

        public String getError() {
            return error;
        }
    }

    public static class PredictResult {
        private final boolean success;
        private final float probability;
        private final long inferenceTimeMs;
        private final String errorCode;
        private final String errorMessage;
        private final String model;

        public PredictResult(boolean success, float probability, long inferenceTimeMs,
                String errorCode, String errorMessage, String model) {
            this.success = success;
            this.probability = probability;
            this.inferenceTimeMs = inferenceTimeMs;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.model = model;
        }

        public boolean isSuccess() {
            return success;
        }

        public float getProbability() {
            return probability;
        }

        public long getInferenceTimeMs() {
            return inferenceTimeMs;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getModel() {
            return model;
        }
    }

    public static class SessionException extends RuntimeException {
        public SessionException(String message) {
            super(message);
        }

        public SessionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AuthenticationException extends SessionException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}