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
