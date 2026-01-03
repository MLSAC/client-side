package wtf.mlsac.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import wtf.mlsac.checks.AICheck;
import wtf.mlsac.session.ISessionManager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class HitListener extends PacketListenerAbstract {
    
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;
    
    public HitListener(ISessionManager sessionManager, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
    }
    
    public void setCurrentTick(int tick) {
    }
    
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }
        
        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        
        if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }
        
        Player attacker = (Player) event.getPlayer();
        if (attacker == null) {
            return;
        }
        
        int targetId = packet.getEntityId();
        Entity target = getEntityById(attacker, targetId);
        
        if (target == null) {
            return;
        }
        
        if (!(target instanceof Player)) {
            return;
        }

        if (aiCheck != null) {
            aiCheck.onAttack(attacker, target);
        }
        
        sessionManager.onAttack(attacker);
    }
    
    private Entity getEntityById(Player attacker, int entityId) {
        for (Entity entity : attacker.getWorld().getEntities()) {
            if (entity.getEntityId() == entityId) {
                return entity;
            }
        }
        return null;
    }
}
