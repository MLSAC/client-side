/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

/**
 * WorldGuard integration for region-based AI check bypass.
 * Supports priority-based region checking - if player is in multiple regions,
 * the highest priority region determines if AI check is disabled.
 */
public class WorldGuardCompat {
    
    private final Logger logger;
    private final boolean enabled;
    private final Map<String, Set<String>> disabledRegions; // world -> regions
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
                // If no world specified, apply to all worlds
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
    
    /**
     * Check if AI check should be bypassed for player at their current location.
     * Uses WorldGuard region priorities - highest priority region determines the result.
     * 
     * @param player The player to check
     * @return true if AI check should be SKIPPED (player is in disabled region with highest priority)
     */
    public boolean shouldBypassAICheck(Player player) {
        if (!enabled || !worldGuardAvailable) {
            return false;
        }
        
        return shouldBypassAtLocation(player.getLocation());
    }
    
    /**
     * Check if AI check should be bypassed at specific location.
     */
    public boolean shouldBypassAtLocation(Location location) {
        if (!enabled || !worldGuardAvailable) {
            return false;
        }
        
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        String worldName = world.getName().toLowerCase();
        
        // Get disabled regions for this world and global (*)
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
            
            // Find the highest priority region and check if it's disabled
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
    
    /**
     * Get all regions at player's location (for debugging).
     */
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
