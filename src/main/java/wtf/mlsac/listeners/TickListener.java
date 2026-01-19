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
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.compat.EventCompat;
import wtf.mlsac.session.ISessionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
public class TickListener {
    private final ISessionManager sessionManager;
    private final AICheck aiCheck;
    private final EventCompat.TickHandler tickHandler;
    private HitListener hitListener;
    public TickListener(JavaPlugin plugin, ISessionManager sessionManager, AICheck aiCheck) {
        this.sessionManager = sessionManager;
        this.aiCheck = aiCheck;
        this.tickHandler = EventCompat.createTickHandler(plugin, this::onTick);
    }
    public void start() {
        tickHandler.start();
    }
    public void stop() {
        tickHandler.stop();
    }
    public void setHitListener(HitListener hitListener) {
        this.hitListener = hitListener;
    }
    private void onTick() {
        int currentTick = tickHandler.getCurrentTick();
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
        return tickHandler.getCurrentTick();
    }
}