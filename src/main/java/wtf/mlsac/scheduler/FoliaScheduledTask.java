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
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
public class FoliaScheduledTask implements wtf.mlsac.scheduler.ScheduledTask {
    private final ScheduledTask task;
    private volatile boolean cancelled = false;
    public FoliaScheduledTask(ScheduledTask task) {
        this.task = task;
    }
    @Override
    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            task.cancel();
        }
    }
    @Override
    public boolean isCancelled() {
        return cancelled || task.isCancelled();
    }
    @Override
    public boolean isRunning() {
        return !isCancelled();
    }
}