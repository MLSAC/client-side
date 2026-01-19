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


package wtf.mlsac.compat;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.logging.Logger;
public class WorldGuardCompat {
    private final Logger logger;
    private final boolean enabled;
    private final Map<String, Set<String>> disabledRegions;
    private boolean worldGuardAvailable;
    public WorldGuardCompat(Logger logger, boolean enabled, List<String> disabledRegionsList) {
        this.logger = logger;
        this.enabled = enabled;
        this.disabledRegions = new HashMap<>();
        parseDisabledRegions(disabledRegionsList);
        checkWorldGuardAvailability();
    }
    private void parseDisabledRegions(List<String> regionsList) {
        for (String entry : regionsList) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                String worldName = parts[0].toLowerCase();
                String regionName = parts[1].toLowerCase();
                disabledRegions.computeIfAbsent(worldName, k -> new HashSet<>()).add(regionName);
            } else {
                disabledRegions.computeIfAbsent("*", k -> new HashSet<>()).add(entry.toLowerCase());
            }
        }
    }
    private void checkWorldGuardAvailability() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                worldGuardAvailable = true;
                logger.info("[WorldGuard] Integration enabled");
            } else {
                worldGuardAvailable = false;
                if (enabled) {
                    logger.warning("[WorldGuard] Plugin not found, region checking disabled");
                }
            }
        } catch (ClassNotFoundException e) {
            worldGuardAvailable = false;
            if (enabled) {
                logger.warning("[WorldGuard] Not available, region checking disabled");
            }
        }
    }
    public boolean shouldBypassAICheck(Player player) {
        if (!enabled || !worldGuardAvailable) {
            return false;
        }
        return shouldBypassAtLocation(player.getLocation());
    }
    public boolean shouldBypassAtLocation(Location location) {
        if (!enabled || !worldGuardAvailable) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        String worldName = world.getName().toLowerCase();
        Set<String> worldDisabled = disabledRegions.getOrDefault(worldName, Collections.emptySet());
        Set<String> globalDisabled = disabledRegions.getOrDefault("*", Collections.emptySet());
        if (worldDisabled.isEmpty() && globalDisabled.isEmpty()) {
            return false;
        }
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager == null) {
                return false;
            }
            ApplicableRegionSet regions = regionManager.getApplicableRegions(
                BukkitAdapter.asBlockVector(location)
            );
            ProtectedRegion highestPriorityRegion = null;
            int highestPriority = Integer.MIN_VALUE;
            for (ProtectedRegion region : regions) {
                if (region.getPriority() > highestPriority) {
                    highestPriority = region.getPriority();
                    highestPriorityRegion = region;
                }
            }
            if (highestPriorityRegion != null) {
                String regionId = highestPriorityRegion.getId().toLowerCase();
                return worldDisabled.contains(regionId) || globalDisabled.contains(regionId);
            }
        } catch (Exception e) {
            logger.warning("[WorldGuard] Error checking regions: " + e.getMessage());
        }
        return false;
    }
    public List<String> getRegionsAtPlayer(Player player) {
        List<String> result = new ArrayList<>();
        if (!worldGuardAvailable) {
            return result;
        }
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return result;
        }
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager == null) {
                return result;
            }
            ApplicableRegionSet regions = regionManager.getApplicableRegions(
                BukkitAdapter.asBlockVector(location)
            );
            for (ProtectedRegion region : regions) {
                result.add(region.getId() + " (priority: " + region.getPriority() + ")");
            }
        } catch (Exception e) {
            logger.warning("[WorldGuard] Error getting regions: " + e.getMessage());
        }
        return result;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public boolean isWorldGuardAvailable() {
        return worldGuardAvailable;
    }
    public Map<String, Set<String>> getDisabledRegions() {
        return Collections.unmodifiableMap(disabledRegions);
    }
}