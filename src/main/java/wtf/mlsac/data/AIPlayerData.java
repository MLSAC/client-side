package wtf.mlsac.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import wtf.mlsac.util.AimProcessor;
import wtf.mlsac.util.BufferCalculator;

public class AIPlayerData {
    
    private final UUID playerId;
    
    private final AimProcessor aimProcessor;
    
    private final Deque<TickData> tickBuffer;
    private final int sequence;
    
    private int ticksSinceAttack;
    
    private int ticksStep;
    
    private double buffer;
    private double lastProbability;
    
    public AIPlayerData(UUID playerId) {
        this(playerId, 40);
    }
    
    public AIPlayerData(UUID playerId, int sequence) {
        this.playerId = playerId;
        this.sequence = sequence;
        this.aimProcessor = new AimProcessor();
        this.tickBuffer = new ArrayDeque<>(sequence);
        this.ticksSinceAttack = sequence + 1;
        this.ticksStep = 0;
        this.buffer = 0.0;
        this.lastProbability = 0.0;
    }

    public TickData processTick(float yaw, float pitch) {
        TickData tickData = aimProcessor.process(yaw, pitch);
        
        if (tickBuffer.size() >= sequence) {
            tickBuffer.pollFirst();
        }
        
        tickBuffer.addLast(tickData);
        
        return tickData;
    }
    
    public void onAttack() {
        this.ticksSinceAttack = 0;
    }
    
    public void incrementTicksSinceAttack() {
        if (this.ticksSinceAttack <= sequence + 1) {
            this.ticksSinceAttack++;
        }
    }
    
    public void incrementStepCounter() {
        this.ticksStep++;
    }
    
    @Deprecated
    public void onTick() {
        ticksSinceAttack++;
        ticksStep++;
        
        if (ticksSinceAttack > sequence) {
            clearBuffer();
        }
    }
    
    public boolean shouldSendData(int step, int sequence) {
        return ticksStep >= step && tickBuffer.size() >= sequence;
    }
    
    @Deprecated
    public boolean shouldSendData(int step) {
        return ticksStep >= step && tickBuffer.size() >= sequence && ticksSinceAttack <= sequence;
    }
    
    public void resetStepCounter() {
        this.ticksStep = 0;
    }
    
    public List<TickData> getTickBuffer() {
        return new ArrayList<>(tickBuffer);
    }
    
    public void clearBuffer() {
        tickBuffer.clear();
    }
    
    public void fullReset() {
        tickBuffer.clear();
        aimProcessor.reset();
    }
    
    public boolean isInCombat() {
        return ticksSinceAttack <= sequence;
    }
    
    public int getBufferSize() {
        return tickBuffer.size();
    }
    
    public int getSequence() {
        return sequence;
    }
    
    public int getTicksSinceAttack() {
        return ticksSinceAttack;
    }
    
    public void updateBuffer(double probability, double multiplier, double decreaseAmount, double threshold) {
        this.lastProbability = probability;
        this.buffer = BufferCalculator.updateBuffer(buffer, probability, multiplier, decreaseAmount, threshold);
    }
    
    public boolean shouldFlag(double flagThreshold) {
        return BufferCalculator.shouldFlag(buffer, flagThreshold);
    }
    
    public void resetBuffer(double resetValue) {
        this.buffer = BufferCalculator.resetBuffer(resetValue);
    }
    
    public UUID getPlayerId() { return playerId; }
    public double getBuffer() { return buffer; }
    public double getLastProbability() { return lastProbability; }
    public AimProcessor getAimProcessor() { return aimProcessor; }
}
