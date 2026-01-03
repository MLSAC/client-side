package wtf.mlsac.data;

import java.util.Locale;
import java.util.StringJoiner;

public final class TickData {
    
    public final float deltaYaw;
    public final float deltaPitch;
    public final float accelYaw;
    public final float accelPitch;
    public final float jerkYaw;
    public final float jerkPitch;
    public final float gcdErrorYaw;
    public final float gcdErrorPitch;
    
    public TickData(float deltaYaw, float deltaPitch, 
                    float accelYaw, float accelPitch,
                    float jerkYaw, float jerkPitch,
                    float gcdErrorYaw, float gcdErrorPitch) {
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
        this.accelYaw = accelYaw;
        this.accelPitch = accelPitch;
        this.jerkYaw = jerkYaw;
        this.jerkPitch = jerkPitch;
        this.gcdErrorYaw = gcdErrorYaw;
        this.gcdErrorPitch = gcdErrorPitch;
    }
    
    public static String getHeader() {
        return "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch,"
            + "gcd_error_yaw,gcd_error_pitch";
    }
    
    public String toCsv(String status) {
        int cheatingStatus = status.equalsIgnoreCase("CHEAT") ? 1 : 0;
        StringJoiner joiner = new StringJoiner(",");
        joiner.add(String.valueOf(cheatingStatus));
        joiner.add(String.format(Locale.US, "%.6f", deltaYaw));
        joiner.add(String.format(Locale.US, "%.6f", deltaPitch));
        joiner.add(String.format(Locale.US, "%.6f", accelYaw));
        joiner.add(String.format(Locale.US, "%.6f", accelPitch));
        joiner.add(String.format(Locale.US, "%.6f", jerkYaw));
        joiner.add(String.format(Locale.US, "%.6f", jerkPitch));
        joiner.add(String.format(Locale.US, "%.6f", gcdErrorYaw));
        joiner.add(String.format(Locale.US, "%.6f", gcdErrorPitch));
        return joiner.toString();
    }
    
    @Override
    public String toString() {
        return String.format("TickData[dYaw=%.4f, dPitch=%.4f, aYaw=%.4f, aPitch=%.4f, jYaw=%.4f, jPitch=%.4f, gcdYaw=%.4f, gcdPitch=%.4f]",
            deltaYaw, deltaPitch, accelYaw, accelPitch, jerkYaw, jerkPitch, gcdErrorYaw, gcdErrorPitch);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TickData)) return false;
        TickData other = (TickData) obj;
        return Float.compare(deltaYaw, other.deltaYaw) == 0
            && Float.compare(deltaPitch, other.deltaPitch) == 0
            && Float.compare(accelYaw, other.accelYaw) == 0
            && Float.compare(accelPitch, other.accelPitch) == 0
            && Float.compare(jerkYaw, other.jerkYaw) == 0
            && Float.compare(jerkPitch, other.jerkPitch) == 0
            && Float.compare(gcdErrorYaw, other.gcdErrorYaw) == 0
            && Float.compare(gcdErrorPitch, other.gcdErrorPitch) == 0;
    }
    
    @Override
    public int hashCode() {
        int result = Float.hashCode(deltaYaw);
        result = 31 * result + Float.hashCode(deltaPitch);
        result = 31 * result + Float.hashCode(accelYaw);
        result = 31 * result + Float.hashCode(accelPitch);
        result = 31 * result + Float.hashCode(jerkYaw);
        result = 31 * result + Float.hashCode(jerkPitch);
        result = 31 * result + Float.hashCode(gcdErrorYaw);
        result = 31 * result + Float.hashCode(gcdErrorPitch);
        return result;
    }
}
