package wtf.mlsac.datacollector;

import org.bukkit.entity.Player;

import wtf.mlsac.config.Label;
import wtf.mlsac.data.DataSession;
import wtf.mlsac.session.ISessionManager;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class NoOpSessionManager implements ISessionManager {
    
    @Override
    public DataSession startSession(Player player, Label label, String comment) {
        return null;
    }
    
    @Override
    public void stopSession(Player player) {
    }
    
    @Override
    public void stopSession(UUID playerId) {
    }
    
    @Override
    public void stopAllSessions() {
    }
    
    @Override
    public boolean hasActiveSession(Player player) {
        return false;
    }
    
    @Override
    public boolean hasActiveSession(UUID playerId) {
        return false;
    }
    
    @Override
    public DataSession getSession(UUID playerId) {
        return null;
    }
    
    @Override
    public DataSession getSession(Player player) {
        return null;
    }
    
    @Override
    public Collection<DataSession> getActiveSessions() {
        return Collections.emptyList();
    }
    
    @Override
    public int getActiveSessionCount() {
        return 0;
    }

    
    @Override
    public String getCurrentSessionFolder() {
        return null;
    }
    
    @Override
    public void onAttack(Player player) {
    }
    
    @Override
    public void onTick(Player player, float yaw, float pitch) {
    }
}
