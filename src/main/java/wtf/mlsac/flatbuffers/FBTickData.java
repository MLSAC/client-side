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

package wtf.mlsac.flatbuffers;

import com.google.flatbuffers.FlatBufferBuilder;

public final class FBTickData {
    
    public static void startFBTickData(FlatBufferBuilder builder) {
        builder.startTable(8);
    }
    
    public static void addDeltaYaw(FlatBufferBuilder builder, float deltaYaw) {
        builder.addFloat(0, deltaYaw, 0.0f);
    }
    
    public static void addDeltaPitch(FlatBufferBuilder builder, float deltaPitch) {
        builder.addFloat(1, deltaPitch, 0.0f);
    }
    
    public static void addAccelYaw(FlatBufferBuilder builder, float accelYaw) {
        builder.addFloat(2, accelYaw, 0.0f);
    }
    
    public static void addAccelPitch(FlatBufferBuilder builder, float accelPitch) {
        builder.addFloat(3, accelPitch, 0.0f);
    }
    
    public static void addJerkPitch(FlatBufferBuilder builder, float jerkPitch) {
        builder.addFloat(4, jerkPitch, 0.0f);
    }
    
    public static void addJerkYaw(FlatBufferBuilder builder, float jerkYaw) {
        builder.addFloat(5, jerkYaw, 0.0f);
    }
    
    public static void addGcdErrorYaw(FlatBufferBuilder builder, float gcdErrorYaw) {
        builder.addFloat(6, gcdErrorYaw, 0.0f);
    }
    
    public static void addGcdErrorPitch(FlatBufferBuilder builder, float gcdErrorPitch) {
        builder.addFloat(7, gcdErrorPitch, 0.0f);
    }
    
    public static int endFBTickData(FlatBufferBuilder builder) {
        return builder.endTable();
    }
}
