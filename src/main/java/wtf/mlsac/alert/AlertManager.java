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

package wtf.mlsac.alert;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import wtf.mlsac.Permissions;
import wtf.mlsac.config.Config;
import wtf.mlsac.util.ColorUtil;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public class AlertManager {
    
    private final Logger logger;
    private final Set<UUID> playersWithAlerts;
    
    private Config config;
    
    public AlertManager(org.bukkit.plugin.java.JavaPlugin plugin, Config config) {
        this.config = config;
        this.logger = plugin.getLogger();
        this.playersWithAlerts = new CopyOnWriteArraySet<>();
    }
    
    private String getPrefix() {
        return ColorUtil.colorize(config.getPrefix());
    }
    
    public void setConfig(Config config) {
        this.config = config;
    }
    
    public boolean toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (playersWithAlerts.contains(uuid)) {
            playersWithAlerts.remove(uuid);
            String msg = ColorUtil.colorize(config.getMessage("alerts-disabled"));
            player.sendMessage(getPrefix() + msg);
            return false;
        } else {
            playersWithAlerts.add(uuid);
            String msg = ColorUtil.colorize(config.getMessage("alerts-enabled"));
            player.sendMessage(getPrefix() + msg);
            return true;
        }
    }
    
    public void enableAlerts(Player player) {
        playersWithAlerts.add(player.getUniqueId());
    }
    
    public void disableAlerts(Player player) {
        playersWithAlerts.remove(player.getUniqueId());
    }
    
    public boolean hasAlertsEnabled(Player player) {
        return playersWithAlerts.contains(player.getUniqueId());
    }
    
    private boolean canReceiveAlerts(Player player) {
        return player.hasPermission(Permissions.ALERTS) || player.hasPermission(Permissions.ADMIN);
    }
    
    public void sendAlert(String suspectName, double probability, double buffer) {
        String message = formatAlertMessage(suspectName, probability, buffer);
        
        for (UUID uuid : playersWithAlerts) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && canReceiveAlerts(player)) {
                player.sendMessage(message);
            }
        }
        
        if (config.isAiConsoleAlerts()) {
            logger.info(ColorUtil.stripColors(message));
        }
    }
    
    public void sendAlert(String suspectName, double probability, double buffer, int vl) {
        String message = formatAlertMessage(suspectName, probability, buffer, vl);
        
        for (UUID uuid : playersWithAlerts) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && canReceiveAlerts(player)) {
                player.sendMessage(message);
            }
        }
        
        if (config.isAiConsoleAlerts()) {
            logger.info(ColorUtil.stripColors(message));
        }
    }
    
    private String formatAlertMessage(String suspectName, double probability, double buffer) {
        String template = config.getMessage("alert-format", suspectName, probability, buffer, 0);
        return getPrefix() + ColorUtil.colorize(template);
    }
    
    private String formatAlertMessage(String suspectName, double probability, double buffer, int vl) {
        String template = config.getMessage("alert-format-vl", suspectName, probability, buffer, vl);
        return getPrefix() + ColorUtil.colorize(template);
    }
    
    public void handlePlayerQuit(Player player) {
        playersWithAlerts.remove(player.getUniqueId());
    }
    
    public boolean shouldAlert(double probability) {
        return probability >= config.getAiAlertThreshold();
    }
    
    public double getAlertThreshold() {
        return config.getAiAlertThreshold();
    }
}
