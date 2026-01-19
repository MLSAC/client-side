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
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Bukkit;
public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }
    @Override
    public ScheduledTask runSync(Runnable task) {
        BukkitTask bukkitTask = scheduler.runTask(plugin, task);
        return new BukkitScheduledTask(bukkitTask);
    }
    @Override
    public ScheduledTask runSyncDelayed(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = scheduler.runTaskLater(plugin, task, delayTicks);
        return new BukkitScheduledTask(bukkitTask);
    }
    @Override
    public ScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks);
        return new BukkitScheduledTask(bukkitTask);
    }
    @Override
    public ScheduledTask runAsync(Runnable task) {
        BukkitTask bukkitTask = scheduler.runTaskAsynchronously(plugin, task);
        return new BukkitScheduledTask(bukkitTask);
    }
    @Override
    public ScheduledTask runAsyncDelayed(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks);
        return new BukkitScheduledTask(bukkitTask);
    }
    @Override
    public ScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = scheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return new BukkitScheduledTask(bukkitTask);
    }
    @Override
    public ScheduledTask runEntitySync(Entity entity, Runnable task) {
        return runSync(task);
    }
    @Override
    public ScheduledTask runEntitySyncDelayed(Entity entity, Runnable task, long delayTicks) {
        return runSyncDelayed(task, delayTicks);
    }
    @Override
    public ScheduledTask runEntitySyncRepeating(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return runSyncRepeating(task, delayTicks, periodTicks);
    }
    @Override
    public ScheduledTask runRegionSync(Location location, Runnable task) {
        return runSync(task);
    }
    @Override
    public ScheduledTask runRegionSyncDelayed(Location location, Runnable task, long delayTicks) {
        return runSyncDelayed(task, delayTicks);
    }
    @Override
    public ScheduledTask runRegionSyncRepeating(Location location, Runnable task, long delayTicks, long periodTicks) {
        return runSyncRepeating(task, delayTicks, periodTicks);
    }
    @Override
    public ServerType getServerType() {
        return ServerType.BUKKIT;
    }
}