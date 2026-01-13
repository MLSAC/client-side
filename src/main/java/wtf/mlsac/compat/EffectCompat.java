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

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public final class EffectCompat {

    private static final Map<String, PotionEffectType> effectCache = new HashMap<>();

    // Effect name mappings: legacy -> modern (1.20.5+)
    private static final Map<String, String> LEGACY_TO_MODERN = new HashMap<>();
    private static final Map<String, String> MODERN_TO_LEGACY = new HashMap<>();

    static {
        // Initialize mappings
        addMapping("SLOW", "SLOWNESS");
        addMapping("JUMP", "JUMP_BOOST");
        addMapping("CONFUSION", "NAUSEA");
        addMapping("DAMAGE_RESISTANCE", "RESISTANCE");
        addMapping("FAST_DIGGING", "HASTE");
        addMapping("SLOW_DIGGING", "MINING_FATIGUE");
        addMapping("INCREASE_DAMAGE", "STRENGTH");
        addMapping("HEAL", "INSTANT_HEALTH");
        addMapping("HARM", "INSTANT_DAMAGE");
    }

    private static void addMapping(String legacy, String modern) {
        LEGACY_TO_MODERN.put(legacy, modern);
        MODERN_TO_LEGACY.put(modern, legacy);
    }

    private EffectCompat() {
    }

    public static PotionEffectType getEffect(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Check cache first
        if (effectCache.containsKey(name)) {
            return effectCache.get(name);
        }

        PotionEffectType effect = resolveEffect(name);
        effectCache.put(name, effect);
        return effect;
    }

    private static PotionEffectType resolveEffect(String name) {
        // Try direct lookup first
        PotionEffectType direct = PotionEffectType.getByName(name);
        if (direct != null) {
            return direct;
        }

        VersionAdapter adapter = VersionAdapter.get();

        // If on modern version (1.20.5+), try converting legacy name to modern
        if (adapter.isAtLeast(ServerVersion.V1_20_5)) {
            String modernName = LEGACY_TO_MODERN.get(name);
            if (modernName != null) {
                PotionEffectType modern = PotionEffectType.getByName(modernName);
                if (modern != null) {
                    return modern;
                }
            }
        } else {
            // On legacy version, try converting modern name to legacy
            String legacyName = MODERN_TO_LEGACY.get(name);
            if (legacyName != null) {
                PotionEffectType legacy = PotionEffectType.getByName(legacyName);
                if (legacy != null) {
                    return legacy;
                }
            }
        }

        return null;
    }

    public static PotionEffectType getSlowness() {
        return getEffect(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "SLOWNESS" : "SLOW");
    }

    public static PotionEffectType getJumpBoost() {
        return getEffect(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "JUMP_BOOST" : "JUMP");
    }

    public static PotionEffectType getLevitation() {
        return getEffect("LEVITATION");
    }

    public static PotionEffectType getResistance() {
        return getEffect(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "RESISTANCE" : "DAMAGE_RESISTANCE");
    }

    public static PotionEffectType getNausea() {
        return getEffect(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "NAUSEA" : "CONFUSION");
    }

    public static PotionEffectType getHaste() {
        return getEffect(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "HASTE" : "FAST_DIGGING");
    }

    public static PotionEffectType getStrength() {
        return getEffect(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "STRENGTH" : "INCREASE_DAMAGE");
    }

    public static boolean applyEffect(Player player, PotionEffectType type,
                                       int duration, int amplifier,
                                       boolean ambient, boolean particles) {
        if (player == null || type == null) {
            return false;
        }
        try {
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void removeEffect(Player player, PotionEffectType type) {
        if (player == null || type == null) {
            return;
        }
        try {
            player.removePotionEffect(type);
        } catch (Exception ignored) {
        }
    }

    static void clearCache() {
        effectCache.clear();
    }
}
