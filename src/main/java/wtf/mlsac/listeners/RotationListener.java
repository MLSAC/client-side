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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.session.ISessionManager;
import org.bukkit.entity.Player;
public class RotationListener extends PacketListenerAbstract {
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;
    public RotationListener(ISessionManager sessionManager, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
    }
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                return;
            }
            Player player = (Player) event.getPlayer();
            if (player == null) {
                return;
            }
            WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
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
            // Silently ignore packet errors to prevent kicks
        }
    }
}