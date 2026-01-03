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
