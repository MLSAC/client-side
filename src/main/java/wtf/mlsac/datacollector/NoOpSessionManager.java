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
