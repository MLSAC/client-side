package wtf.mlsac.session;

import org.bukkit.entity.Player;

import wtf.mlsac.config.Label;
import wtf.mlsac.data.DataSession;

import java.util.Collection;
import java.util.UUID;

public interface ISessionManager {
    
    DataSession startSession(Player player, Label label, String comment);

    void stopSession(Player player);
    
    void stopSession(UUID playerId);
    
    void stopAllSessions();
    
    boolean hasActiveSession(Player player);
    
    boolean hasActiveSession(UUID playerId);
    
    DataSession getSession(UUID playerId);
    
    DataSession getSession(Player player);
    
    Collection<DataSession> getActiveSessions();
    
    int getActiveSessionCount();

    String getCurrentSessionFolder();
    
    void onAttack(Player player);
    
    void onTick(Player player, float yaw, float pitch);
}
