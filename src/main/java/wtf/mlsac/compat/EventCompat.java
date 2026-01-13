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

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EventCompat {

    private static boolean hasServerTickEndEvent = false;

    static {
        try {
            Class.forName("com.destroystokyo.paper.event.server.ServerTickEndEvent");
            hasServerTickEndEvent = true;
        } catch (ClassNotFoundException e) {
            hasServerTickEndEvent = false;
        }
    }

    private EventCompat() {
    }

    public static boolean hasServerTickEndEvent() {
        return hasServerTickEndEvent;
    }

    public static TickHandler createTickHandler(JavaPlugin plugin, Runnable onTick) {
        if (hasServerTickEndEvent) {
            return new PaperTickHandler(plugin, onTick);
        } else {
            return new SpigotTickHandler(plugin, onTick);
        }
    }

    public interface TickHandler {
        void start();

        void stop();

        int getCurrentTick();
    }

    private static class PaperTickHandler implements TickHandler, Listener {
        private final JavaPlugin plugin;
        private final Runnable onTick;
        private int currentTick = 0;
        private boolean running = false;

        PaperTickHandler(JavaPlugin plugin, Runnable onTick) {
            this.plugin = plugin;
            this.onTick = onTick;
        }

        @Override
        public void start() {
            if (!running) {
                Bukkit.getPluginManager().registerEvents(this, plugin);
                running = true;
            }
        }

        @Override
        public void stop() {
            if (running) {
                HandlerList.unregisterAll(this);
                running = false;
            }
        }

        @Override
        public int getCurrentTick() {
            return currentTick;
        }

        @org.bukkit.event.EventHandler
        public void onServerTick(com.destroystokyo.paper.event.server.ServerTickEndEvent event) {
            currentTick++;
            if (onTick != null) {
                onTick.run();
            }
        }
    }

    private static class SpigotTickHandler implements TickHandler {
        private final JavaPlugin plugin;
        private final Runnable onTick;
        private BukkitTask task;
        private int currentTick = 0;

        SpigotTickHandler(JavaPlugin plugin, Runnable onTick) {
            this.plugin = plugin;
            this.onTick = onTick;
        }

        @Override
        public void start() {
            if (task == null) {
                task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    currentTick++;
                    if (onTick != null) {
                        onTick.run();
                    }
                }, 0L, 1L);
            }
        }

        @Override
        public void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }

        @Override
        public int getCurrentTick() {
            return currentTick;
        }
    }
}
