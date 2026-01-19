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


package wtf.mlsac.util;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
public class FeatureCalculator {
    public double calculateDelta(double current, double previous) {
        double delta = current - previous;
        return normalizeAngle(delta);
    }
    public double calculateAcceleration(double currentDelta, double previousDelta) {
        return currentDelta - previousDelta;
    }
    public double calculateJerk(double currentAccel, double previousAccel) {
        return currentAccel - previousAccel;
    }
    public double calculateAngleToTarget(Player player, Entity target) {
        if (target == null || !target.isValid()) {
            return 0.0;
        }
        Location playerEyeLocation = player.getEyeLocation();
        Location targetLocation = target.getLocation();
        targetLocation = targetLocation.add(0, target.getHeight() / 2.0, 0);
        Vector lookDirection = playerEyeLocation.getDirection().normalize();
        Vector toTarget = targetLocation.toVector().subtract(playerEyeLocation.toVector()).normalize();
        double dotProduct = lookDirection.dot(toTarget);
        dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct));
        return Math.toDegrees(Math.acos(dotProduct));
    }
    public double calculateAngleReductionSpeed(double currentAngle, double previousAngle) {
        return currentAngle - previousAngle;
    }
    public double calculateStandardDeviation(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        int n = values.length;
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        double mean = sum / n;
        double varianceSum = 0.0;
        for (double value : values) {
            double diff = value - mean;
            varianceSum += diff * diff;
        }
        double variance = varianceSum / n;
        return Math.sqrt(variance);
    }
    public double normalizeAngle(double angle) {
        angle = angle % 360.0;
        if (angle > 180.0) {
            angle -= 360.0;
        } else if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }
}