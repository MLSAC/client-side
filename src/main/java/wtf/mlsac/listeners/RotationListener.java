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

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.session.ISessionManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RotationListener extends PacketListenerAbstract {
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;

    private static class MojangFixState {
        public Vector3d lastClaimedPosition = null;
        public boolean packetPlayerOnGround = false;
        public boolean lastPacketWasTeleport = true;
    }

    private final Map<UUID, MojangFixState> fixStates = new ConcurrentHashMap<>();

    public RotationListener(ISessionManager sessionManager, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        if (event.getUser() != null && event.getUser().getUUID() != null) {
            fixStates.remove(event.getUser().getUUID());
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            Player player = (Player) event.getPlayer();
            if (player == null) {
                return;
            }

            if (event.getPacketType() == PacketType.Play.Client.TELEPORT_CONFIRM) {
                MojangFixState state = fixStates.computeIfAbsent(player.getUniqueId(), k -> new MojangFixState());
                state.lastPacketWasTeleport = true;
                return;
            }

            if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                return;
            }

            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
            MojangFixState state = fixStates.computeIfAbsent(player.getUniqueId(), k -> new MojangFixState());

            boolean wasTeleport = state.lastPacketWasTeleport;
            if (wasTeleport) {
                state.lastPacketWasTeleport = false;
            }

            ClientVersion clientVersion = PacketEvents.getAPI().getPlayerManager().getClientVersion(event.getUser());
            boolean isAffectedVersion = clientVersion != null &&
                    clientVersion.isNewerThanOrEquals(ClientVersion.V_1_17) &&
                    !clientVersion.isNewerThanOrEquals(ClientVersion.V_1_21);

            if (isAffectedVersion) {
                Vector3d position = packet.getLocation().getPosition();
                double threshold = 0.03125;
                boolean inVehicle = player.isInsideVehicle();
                boolean hasMovementAndRotation = packet.hasPositionChanged() && packet.hasRotationChanged();

                boolean sameGroundAndCloseClaim = false;
                if (state.lastClaimedPosition != null) {
                    double distanceSq = state.lastClaimedPosition.distanceSquared(position);
                    sameGroundAndCloseClaim = packet.isOnGround() == state.packetPlayerOnGround &&
                            distanceSq < threshold * threshold;
                }

                boolean shouldProcessDuplicate = !wasTeleport && hasMovementAndRotation &&
                        (sameGroundAndCloseClaim || inVehicle);

                if (shouldProcessDuplicate) {
                    event.setCancelled(true);

                    if (packet.hasPositionChanged()) {
                        state.lastClaimedPosition = position;
                    }
                    return;
                }
            }

            state.packetPlayerOnGround = packet.isOnGround();
            if (packet.hasPositionChanged()) {
                state.lastClaimedPosition = packet.getLocation().getPosition();
            }

            if (!packet.hasRotationChanged()) {
                return;
            }

            float yaw = packet.getLocation().getYaw();
            float pitch = packet.getLocation().getPitch();
            if (aiCheck != null) {
                aiCheck.onRotationPacket(player, yaw, pitch);
            }
            if (sessionManager.hasActiveSession(player)) {
                sessionManager.onTick(player, yaw, pitch);
            }
        } catch (Exception e) {
        }
    }
}