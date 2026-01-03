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
