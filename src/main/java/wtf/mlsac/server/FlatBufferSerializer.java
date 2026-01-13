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

package wtf.mlsac.server;

import com.google.flatbuffers.FlatBufferBuilder;

import wtf.mlsac.data.TickData;
import wtf.mlsac.flatbuffers.FBTickData;
import wtf.mlsac.flatbuffers.FBTickDataSequence;

import java.nio.ByteBuffer;
import java.util.List;

public class FlatBufferSerializer {
    
    private static final ThreadLocal<FlatBufferBuilder> BUILDER =
        ThreadLocal.withInitial(() -> new FlatBufferBuilder(4096));
    
    public static byte[] serialize(List<TickData> ticks) {
        FlatBufferBuilder builder = BUILDER.get();
        builder.clear();
        
        int[] tickOffsets = new int[ticks.size()];
        
        for (int i = ticks.size() - 1; i >= 0; i--) {
            TickData tick = ticks.get(i);
            
            FBTickData.startFBTickData(builder);
            FBTickData.addDeltaYaw(builder, tick.deltaYaw);
            FBTickData.addDeltaPitch(builder, tick.deltaPitch);
            FBTickData.addAccelYaw(builder, tick.accelYaw);
            FBTickData.addAccelPitch(builder, tick.accelPitch);
            FBTickData.addJerkPitch(builder, tick.jerkPitch);
            FBTickData.addJerkYaw(builder, tick.jerkYaw);
            FBTickData.addGcdErrorYaw(builder, tick.gcdErrorYaw);
            FBTickData.addGcdErrorPitch(builder, tick.gcdErrorPitch);
            tickOffsets[i] = FBTickData.endFBTickData(builder);
        }
        
        int ticksVector = FBTickDataSequence.createTicksVector(builder, tickOffsets);
        FBTickDataSequence.startFBTickDataSequence(builder);
        FBTickDataSequence.addTicks(builder, ticksVector);
        int sequenceOffset = FBTickDataSequence.endFBTickDataSequence(builder);
        builder.finish(sequenceOffset);
        
        ByteBuffer buf = builder.dataBuffer();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        return bytes;
    }
}
