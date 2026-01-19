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
public interface SchedulerAdapter {
    ScheduledTask runSync(Runnable task);
    ScheduledTask runSyncDelayed(Runnable task, long delayTicks);
    ScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks);
    ScheduledTask runAsync(Runnable task);
    ScheduledTask runAsyncDelayed(Runnable task, long delayTicks);
    ScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
    ScheduledTask runEntitySync(Entity entity, Runnable task);
    ScheduledTask runEntitySyncDelayed(Entity entity, Runnable task, long delayTicks);
    ScheduledTask runEntitySyncRepeating(Entity entity, Runnable task, long delayTicks, long periodTicks);
    ScheduledTask runRegionSync(Location location, Runnable task);
    ScheduledTask runRegionSyncDelayed(Location location, Runnable task, long delayTicks);
    ScheduledTask runRegionSyncRepeating(Location location, Runnable task, long delayTicks, long periodTicks);
    ServerType getServerType();
}