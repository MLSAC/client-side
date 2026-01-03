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
