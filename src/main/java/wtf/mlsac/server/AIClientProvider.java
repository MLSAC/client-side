package wtf.mlsac.server;

import org.bukkit.Bukkit;

import wtf.mlsac.Main;
import wtf.mlsac.config.Config;
import wtf.mlsac.signalr.SignalRClient;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class AIClientProvider {
    
    private final Main plugin;
    private final Logger logger;
    
    private IAIClient currentClient;
    private Config config;
    private volatile boolean connecting = false;
    private volatile String clientType = "none";
    
    public AIClientProvider(Main plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }
    
    public CompletableFuture<Boolean> initialize() {
        if (!config.isAiEnabled()) {
            plugin.debug("[AI] AI is disabled, skipping client initialization");
            return CompletableFuture.completedFuture(false);
        }
        
        String serverAddress = config.getServerAddress();
        String apiKey = config.getAiApiKey();
        
        if (serverAddress == null || serverAddress.isEmpty()) {
            logger.warning("[AI] Server address is not configured!");
            return CompletableFuture.completedFuture(false);
        }
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warning("[AI] API key is not configured!");
            return CompletableFuture.completedFuture(false);
        }
        
        connecting = true;
        return initializeSignalR(serverAddress, apiKey);
    }

    private CompletableFuture<Boolean> initializeSignalR(String serverAddress, String apiKey) {
        SignalRClient signalRClient = new SignalRClient(
            plugin,
            serverAddress,
            apiKey,
            config.getReportStatsIntervalSeconds(),
            () -> Bukkit.getOnlinePlayers().size()
        );
        
        this.currentClient = signalRClient;
        this.clientType = "SignalR";
        
        logger.info("[SignalR] Connecting to " + serverAddress + "...");
        
        return signalRClient.connectWithRetry()
            .thenApply(success -> {
                connecting = false;
                if (success) {
                    logger.info("[SignalR] Successfully connected to InferenceServer");
                } else {
                    logger.warning("[SignalR] Failed to connect to InferenceServer");
                    currentClient = null;
                    clientType = "none";
                }
                return success;
            })
            .exceptionally(e -> {
                connecting = false;
                logger.severe("[SignalR] Connection error: " + e.getMessage());
                currentClient = null;
                clientType = "none";
                return false;
            });
    }
    
    public CompletableFuture<Void> shutdown() {
        if (currentClient != null) {
            logger.info("[AI] Shutting down " + clientType + " client...");
            return currentClient.disconnect()
                .thenRun(() -> {
                    currentClient = null;
                    clientType = "none";
                    logger.info("[AI] Client shutdown complete");
                });
        }
        return CompletableFuture.completedFuture(null);
    }
    
    public CompletableFuture<Boolean> reload() {
        return shutdown().thenCompose(v -> initialize());
    }
    
    public void setConfig(Config config) {
        this.config = config;
    }
    
    public IAIClient get() {
        return currentClient;
    }
    
    public boolean isAvailable() {
        return currentClient != null && currentClient.isConnected();
    }
    
    public boolean isEnabled() {
        return config.isAiEnabled();
    }
    
    public boolean isConnecting() {
        return connecting;
    }
    
    public boolean isLimitExceeded() {
        return currentClient != null && currentClient.isLimitExceeded();
    }
    
    public String getClientType() {
        return clientType;
    }
}
