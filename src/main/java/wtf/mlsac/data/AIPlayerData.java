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
    
    private volatile double buffer;
    private volatile double lastProbability;
    
    private volatile boolean pendingRequest;
    
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
        this.pendingRequest = false;
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
    
    public void onTeleport() {
        aimProcessor.reset();
        clearBuffer();
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
        return !pendingRequest && ticksStep >= step && tickBuffer.size() >= sequence;
    }
    
    public void setPendingRequest(boolean pending) {
        this.pendingRequest = pending;
    }
    
    public boolean isPendingRequest() {
        return pendingRequest;
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
        pendingRequest = false;
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
    
    public synchronized void updateBuffer(double probability, double multiplier, double decreaseAmount, double threshold) {
        this.lastProbability = probability;
        this.buffer = BufferCalculator.updateBuffer(buffer, probability, multiplier, decreaseAmount, threshold);
    }
    
    public synchronized boolean shouldFlag(double flagThreshold) {
        return BufferCalculator.shouldFlag(buffer, flagThreshold);
    }
    
    public synchronized void resetBuffer(double resetValue) {
        this.buffer = BufferCalculator.resetBuffer(resetValue);
    }
    
    public UUID getPlayerId() { return playerId; }
    public synchronized double getBuffer() { return buffer; }
    public double getLastProbability() { return lastProbability; }
    public AimProcessor getAimProcessor() { return aimProcessor; }
}
