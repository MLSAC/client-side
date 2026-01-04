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

package wtf.mlsac.penalty;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BanAnimation implements Listener {
    
    private final JavaPlugin plugin;
    private final Set<UUID> animatingPlayers = new HashSet<>();
    
    private static final int LEVITATION_DURATION = 60;
    private static final int TOTAL_ANIMATION_TICKS = 80;
    private static final double LEVITATION_HEIGHT = 2.0;
    
    public BanAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void playAnimation(Player player, String banCommand, PenaltyContext context) {
        if (player == null) return;
        
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> playAnimation(player, banCommand, context));
            return;
        }

        if (!player.isOnline()) {
            executeBanCommand(banCommand);
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        if (animatingPlayers.contains(playerId)) {
            return;
        }
        
        plugin.getLogger().info(">>> STAGE 1: Starting ban animation for " + player.getName());
        animatingPlayers.add(playerId);

        freezePlayer(player);
        
        new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                try {
                    if (!player.isOnline()) {
                        finish();
                        return;
                    }
                    
                    tick++;
                    
                    if (tick <= LEVITATION_DURATION) {
                        spawnRisingParticles(player.getLocation(), tick);
                    }
                    
                    if (tick >= 20 && tick <= 75) {
                        double sphereProgress = (double) (tick - 20) / 55.0;
                        double sphereRadius = 3.0 - (2.0 * sphereProgress);
                        spawnSphereParticles(player.getLocation(), sphereRadius, tick);
                    }
                    
                    if (tick >= TOTAL_ANIMATION_TICKS) {
                        player.getWorld().strikeLightningEffect(player.getLocation());
                        spawnExplosionParticles(player.getLocation());
                        
                        plugin.getLogger().info(">>> STAGE 2: Animation finished, banning " + player.getName());
                        finish();
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("CRITICAL ERROR IN BAN ANIMATION: " + e.getMessage());
                    finish();
                }
            }
            
            private void finish() {
                cancel();
                animatingPlayers.remove(playerId);
                if (player.isOnline()) {
                    unfreezePlayer(player);
                }
                executeBanCommand(banCommand);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void freezePlayer(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, TOTAL_ANIMATION_TICKS + 40, 255, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, TOTAL_ANIMATION_TICKS + 40, 128, false, false));
        
        try {
            PotionEffectType levitation = PotionEffectType.getByName("LEVITATION");
            if (levitation != null) {
                player.addPotionEffect(new PotionEffect(levitation, TOTAL_ANIMATION_TICKS, 1, false, false));
            }
        } catch (Exception ignored) {}

        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }
    
    private void unfreezePlayer(Player player) {
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.JUMP);
    }
    
    private void spawnRisingParticles(Location center, int tick) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians((tick * 15 + i * 45) % 360);
            double radius = 1.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            
            Location particleLoc = center.clone().add(x, -1 + (tick * 0.05), z);
            
            center.getWorld().spawnParticle(Particle.SPELL_WITCH, particleLoc, 2, 0.1, 0.1, 0.1, 0);
            
            if (tick % 3 == 0) {
                center.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0.2, 0.2, 0.2, 0);
            }
        }
        
        double spiralAngle = Math.toRadians(tick * 20);
        double spiralRadius = 0.8;
        Location spiralLoc = center.clone().add(
            Math.cos(spiralAngle) * spiralRadius,
            0,
            Math.sin(spiralAngle) * spiralRadius
        );
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, spiralLoc, 3, 0.05, 0.05, 0.05, 0);
    }

    private void spawnSphereParticles(Location center, double radius, int tick) {
        int particleCount = 20;
        
        for (int i = 0; i < particleCount; i++) {
            double phi = Math.acos(1 - 2.0 * i / particleCount);
            double theta = Math.PI * (1 + Math.sqrt(5)) * i + Math.toRadians(tick * 5);
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);
            
            Location particleLoc = center.clone().add(x, y, z);
            
            center.getWorld().spawnParticle(Particle.SPELL_WITCH, particleLoc, 1, 0, 0, 0, 0);
            
            if (i % 3 == 0) {
                Particle.DustOptions dust = new Particle.DustOptions(
                    org.bukkit.Color.fromRGB(255, 105, 180),
                    1.0f
                );
                center.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0, dust);
            }
        }
        
        center.getWorld().spawnParticle(Particle.END_ROD, center, 5, 0.3, 0.5, 0.3, 0.02);
    }
    
    private void spawnExplosionParticles(Location center) {
        center.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, center, 1, 0, 0, 0, 0);
        
        for (int i = 0; i < 50; i++) {
            Vector dir = new Vector(
                Math.random() * 2 - 1,
                Math.random() * 2 - 1,
                Math.random() * 2 - 1
            ).normalize().multiply(2);
            
            Location particleLoc = center.clone().add(dir);
            center.getWorld().spawnParticle(Particle.SPELL_WITCH, particleLoc, 3, 0.1, 0.1, 0.1, 0);
        }
        
        Particle.DustOptions pinkDust = new Particle.DustOptions(
            org.bukkit.Color.fromRGB(255, 20, 147),
            2.0f
        );
        center.getWorld().spawnParticle(Particle.REDSTONE, center, 100, 1.5, 1.5, 1.5, 0, pinkDust);
        
        center.getWorld().spawnParticle(Particle.SOUL, center, 30, 0.5, 0.5, 0.5, 0.1);
    }
    
    private double easeOutQuad(double t) {
        return 1 - (1 - t) * (1 - t);
    }
    
    private void executeBanCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
    
    public boolean isAnimating(UUID playerId) {
        return animatingPlayers.contains(playerId);
    }
    
    public boolean isAnimating(Player player) {
        return player != null && animatingPlayers.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isAnimating(event.getPlayer())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to == null) return;
            
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                Location newTo = to.clone();
                newTo.setX(from.getX());
                newTo.setZ(from.getZ());
                event.setTo(newTo);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isAnimating(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (isAnimating(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isAnimating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (isAnimating(player)) {
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isAnimating(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isAnimating(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    public void shutdown() {
        HandlerList.unregisterAll(this);
        animatingPlayers.clear();
    }
}
