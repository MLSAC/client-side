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
public final class FBTickDataSequence {
    public static void startFBTickDataSequence(FlatBufferBuilder builder) {
        builder.startTable(1);
    }
    public static void addTicks(FlatBufferBuilder builder, int ticksOffset) {
        builder.addOffset(0, ticksOffset, 0);
    }
    public static int createTicksVector(FlatBufferBuilder builder, int[] data) {
        builder.startVector(4, data.length, 4);
        for (int i = data.length - 1; i >= 0; i--) {
            builder.addOffset(data[i]);
        }
        return builder.endVector();
    }
    public static int endFBTickDataSequence(FlatBufferBuilder builder) {
        return builder.endTable();
    }
    public static void finishFBTickDataSequenceBuffer(FlatBufferBuilder builder, int offset) {
        builder.finish(offset);
    }
}