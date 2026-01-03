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
