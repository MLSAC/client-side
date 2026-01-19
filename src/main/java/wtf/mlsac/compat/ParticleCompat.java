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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import java.util.HashMap;
import java.util.Map;
public final class ParticleCompat {
    private static final Map<String, Particle> particleCache = new HashMap<>();
    private static final Map<String, String> LEGACY_TO_MODERN = new HashMap<>();
    private static final Map<String, String> MODERN_TO_LEGACY = new HashMap<>();
    static {
        addMapping("SPELL_WITCH", "WITCH");
        addMapping("EXPLOSION_HUGE", "EXPLOSION_EMITTER");
        addMapping("REDSTONE", "DUST");
        addMapping("SPELL_MOB", "ENTITY_EFFECT");
        addMapping("SMOKE_NORMAL", "SMOKE");
        addMapping("SMOKE_LARGE", "LARGE_SMOKE");
        addMapping("SPELL", "EFFECT");
        addMapping("SPELL_INSTANT", "INSTANT_EFFECT");
        addMapping("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        addMapping("VILLAGER_ANGRY", "ANGRY_VILLAGER");
        addMapping("WATER_BUBBLE", "BUBBLE");
        addMapping("WATER_SPLASH", "SPLASH");
        addMapping("WATER_WAKE", "FISHING");
        addMapping("SUSPENDED_DEPTH", "UNDERWATER");
        addMapping("CRIT_MAGIC", "ENCHANTED_HIT");
        addMapping("MOB_APPEARANCE", "ELDER_GUARDIAN");
        addMapping("FOOTSTEP", "BLOCK");
        addMapping("ITEM_TAKE", "ITEM");
        addMapping("BLOCK_DUST", "BLOCK");
        addMapping("ITEM_CRACK", "ITEM");
        addMapping("BLOCK_CRACK", "BLOCK");
        addMapping("SNOWBALL", "ITEM_SNOWBALL");
        addMapping("SNOW_SHOVEL", "ITEM_SNOWBALL");
        addMapping("SLIME", "ITEM_SLIME");
        addMapping("DRIP_WATER", "DRIPPING_WATER");
        addMapping("DRIP_LAVA", "DRIPPING_LAVA");
        addMapping("ENCHANTMENT_TABLE", "ENCHANT");
        addMapping("FIREWORKS_SPARK", "FIREWORK");
        addMapping("TOWN_AURA", "MYCELIUM");
        addMapping("EXPLOSION_NORMAL", "POOF");
        addMapping("EXPLOSION_LARGE", "EXPLOSION");
    }
    private static void addMapping(String legacy, String modern) {
        LEGACY_TO_MODERN.put(legacy, modern);
        MODERN_TO_LEGACY.put(modern, legacy);
    }
    private ParticleCompat() {
    }
    public static Particle getParticle(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (particleCache.containsKey(name)) {
            return particleCache.get(name);
        }
        Particle particle = resolveParticle(name);
        particleCache.put(name, particle);
        return particle;
    }
    private static Particle resolveParticle(String name) {
        Particle direct = tryGetParticle(name);
        if (direct != null) {
            return direct;
        }
        VersionAdapter adapter = VersionAdapter.get();
        if (adapter.isAtLeast(ServerVersion.V1_20_5)) {
            String modernName = LEGACY_TO_MODERN.get(name);
            if (modernName != null) {
                Particle modern = tryGetParticle(modernName);
                if (modern != null) {
                    return modern;
                }
            }
        } else {
            String legacyName = MODERN_TO_LEGACY.get(name);
            if (legacyName != null) {
                Particle legacy = tryGetParticle(legacyName);
                if (legacy != null) {
                    return legacy;
                }
            }
        }
        return null;
    }
    private static Particle tryGetParticle(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public static Particle getWitchParticle() {
        return getParticle(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "WITCH" : "SPELL_WITCH");
    }
    public static Particle getExplosionParticle() {
        return getParticle(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "EXPLOSION_EMITTER" : "EXPLOSION_HUGE");
    }
    public static Particle getDustParticle() {
        return getParticle(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "DUST" : "REDSTONE");
    }
    public static Particle getEntityEffectParticle() {
        return getParticle(VersionAdapter.get().isAtLeast(ServerVersion.V1_20_5)
            ? "ENTITY_EFFECT" : "SPELL_MOB");
    }
    public static Particle getSoulParticle() {
        return getParticle("SOUL");
    }
    public static Particle getDragonBreathParticle() {
        return getParticle("DRAGON_BREATH");
    }
    public static Particle getEndRodParticle() {
        return getParticle("END_ROD");
    }
    public static Particle getHeartParticle() {
        return getParticle("HEART");
    }
    public static void spawnParticle(World world, Particle particle, Location loc,
                                      int count, double dx, double dy, double dz, double speed) {
        if (particle == null || world == null || loc == null) {
            return;
        }
        try {
            world.spawnParticle(particle, loc, count, dx, dy, dz, speed);
        } catch (Exception e) {
        }
    }
    public static <T> void spawnParticle(World world, Particle particle, Location loc,
                                          int count, double dx, double dy, double dz,
                                          double speed, T data) {
        if (particle == null || world == null || loc == null) {
            return;
        }
        try {
            world.spawnParticle(particle, loc, count, dx, dy, dz, speed, data);
        } catch (Exception e) {
        }
    }
    static void clearCache() {
        particleCache.clear();
    }
}