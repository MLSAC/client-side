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
public class BufferCalculator {
    private static final double LOW_PROBABILITY_THRESHOLD = 0.1;
    public static double calculateBufferIncrease(double probability, double multiplier, double threshold) {
        if (probability <= threshold) {
            return 0.0;
        }
        return (probability - threshold) * multiplier;
    }
    public static double calculateBufferDecrease(double currentBuffer, double decreaseAmount) {
        return Math.max(0.0, currentBuffer - decreaseAmount);
    }
    public static double updateBuffer(double currentBuffer, double probability, 
                                       double multiplier, double decreaseAmount, double threshold) {
        if (probability > threshold) {
            return currentBuffer + calculateBufferIncrease(probability, multiplier, threshold);
        } else if (probability < LOW_PROBABILITY_THRESHOLD) {
            return calculateBufferDecrease(currentBuffer, decreaseAmount);
        }
        return currentBuffer;
    }
    public static double updateBuffer(double currentBuffer, double probability, 
                                       double multiplier, double decreaseAmount) {
        return updateBuffer(currentBuffer, probability, multiplier, decreaseAmount, 0.5);
    }
    public static boolean shouldFlag(double buffer, double flagThreshold) {
        return buffer >= flagThreshold;
    }
    public static double resetBuffer(double resetValue) {
        return Math.max(0.0, resetValue);
    }
}