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

package wtf.mlsac.scheduler;
import org.bukkit.plugin.Plugin;
public class SchedulerManager {
    private static SchedulerAdapter adapter;
    private static ServerType serverType;
    private static boolean initialized = false;
    public static void initialize(Plugin plugin) {
        if (initialized) {
            throw new IllegalStateException("SchedulerManager is already initialized");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        try {
            serverType = detectServerType();
            if (serverType == ServerType.FOLIA) {
                adapter = new FoliaSchedulerAdapter(plugin);
            } else {
                adapter = new BukkitSchedulerAdapter(plugin);
            }
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SchedulerManager", e);
        }
    }
    public static SchedulerAdapter getAdapter() {
        if (!initialized || adapter == null) {
            throw new IllegalStateException("SchedulerManager has not been initialized. Call initialize(plugin) first.");
        }
        return adapter;
    }
    public static ServerType getServerType() {
        if (!initialized) {
            throw new IllegalStateException("SchedulerManager has not been initialized. Call initialize(plugin) first.");
        }
        return serverType;
    }
    public static boolean isInitialized() {
        return initialized;
    }
    private static ServerType detectServerType() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            return ServerType.FOLIA;
        } catch (ClassNotFoundException e) {
            return ServerType.BUKKIT;
        }
    }
    static void reset() {
        adapter = null;
        serverType = null;
        initialized = false;
    }
}