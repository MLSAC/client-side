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
