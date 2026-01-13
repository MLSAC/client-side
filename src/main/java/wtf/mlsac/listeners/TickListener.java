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

import com.destroystokyo.paper.event.server.ServerTickEndEvent;

import wtf.mlsac.checks.AICheck;
import wtf.mlsac.session.ISessionManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@SuppressWarnings("unused")
public class TickListener implements Listener {
    
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;
    private HitListener hitListener;
    private int currentTick;
    
    public TickListener(ISessionManager sessionManager, AICheck aiCheck) {
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
        this.currentTick = 0;
    }
    
    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }
    
    @EventHandler
    public void onServerTick(ServerTickEndEvent event) {
        currentTick++;
        
        if (hitListener != null) {
            hitListener.setCurrentTick(currentTick);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (aiCheck != null) {
                aiCheck.onTick(player);
            }
        }
    }
    
    public int getCurrentTick() {
        return currentTick;
    }
}
