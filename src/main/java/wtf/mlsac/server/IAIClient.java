package wtf.mlsac.server;

import java.util.concurrent.CompletableFuture;

public interface IAIClient {
    
    CompletableFuture<Boolean> connect();
    
    CompletableFuture<Boolean> connectWithRetry();
    
    CompletableFuture<Void> disconnect();
    
    CompletableFuture<AIResponse> predict(byte[] playerData, String playerUuid);
    
    boolean isConnected();
    
    boolean isLimitExceeded();
    
    String getSessionId();
    
    String getServerAddress();
}
