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
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
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
        lock.writeLock().lock();
        try {
            if (tickBuffer.size() >= sequence) {
                tickBuffer.pollFirst();
            }
            tickBuffer.addLast(tickData);
        } finally {
            lock.writeLock().unlock();
        }
        return tickData;
    }
    public void onAttack() {
        lock.writeLock().lock();
        try {
            this.ticksSinceAttack = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void onTeleport() {
        lock.writeLock().lock();
        try {
            aimProcessor.reset();
            clearBuffer();
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void incrementTicksSinceAttack() {
        lock.writeLock().lock();
        try {
            if (this.ticksSinceAttack <= sequence + 1) {
                this.ticksSinceAttack++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void incrementStepCounter() {
        lock.writeLock().lock();
        try {
            this.ticksStep++;
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Deprecated
    public void onTick() {
        lock.writeLock().lock();
        try {
            ticksSinceAttack++;
            ticksStep++;
            if (ticksSinceAttack > sequence) {
                clearBuffer();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    public boolean shouldSendData(int step, int sequence) {
        lock.readLock().lock();
        try {
            return !pendingRequest && ticksStep >= step && tickBuffer.size() >= sequence;
        } finally {
            lock.readLock().unlock();
        }
    }
    public void setPendingRequest(boolean pending) {
        lock.writeLock().lock();
        try {
            this.pendingRequest = pending;
        } finally {
            lock.writeLock().unlock();
        }
    }
    public boolean isPendingRequest() {
        lock.readLock().lock();
        try {
            return pendingRequest;
        } finally {
            lock.readLock().unlock();
        }
    }
    @Deprecated
    public boolean shouldSendData(int step) {
        lock.readLock().lock();
        try {
            return ticksStep >= step && tickBuffer.size() >= sequence && ticksSinceAttack <= sequence;
        } finally {
            lock.readLock().unlock();
        }
    }
    public void resetStepCounter() {
        lock.writeLock().lock();
        try {
            this.ticksStep = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
    public List<TickData> getTickBuffer() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(tickBuffer);
        } finally {
            lock.readLock().unlock();
        }
    }
    public void clearBuffer() {
        lock.writeLock().lock();
        try {
            tickBuffer.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void fullReset() {
        lock.writeLock().lock();
        try {
            tickBuffer.clear();
            aimProcessor.reset();
            pendingRequest = false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    public boolean isInCombat() {
        lock.readLock().lock();
        try {
            return ticksSinceAttack <= sequence;
        } finally {
            lock.readLock().unlock();
        }
    }
    public int getBufferSize() {
        lock.readLock().lock();
        try {
            return tickBuffer.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    public int getSequence() {
        return sequence;
    }
    public int getTicksSinceAttack() {
        lock.readLock().lock();
        try {
            return ticksSinceAttack;
        } finally {
            lock.readLock().unlock();
        }
    }
    public void updateBuffer(double probability, double multiplier, double decreaseAmount, double threshold) {
        lock.writeLock().lock();
        try {
            this.lastProbability = probability;
            this.buffer = BufferCalculator.updateBuffer(buffer, probability, multiplier, decreaseAmount, threshold);
        } finally {
            lock.writeLock().unlock();
        }
    }
    public boolean shouldFlag(double flagThreshold) {
        lock.readLock().lock();
        try {
            return BufferCalculator.shouldFlag(buffer, flagThreshold);
        } finally {
            lock.readLock().unlock();
        }
    }
    public void resetBuffer(double resetValue) {
        lock.writeLock().lock();
        try {
            this.buffer = BufferCalculator.resetBuffer(resetValue);
        } finally {
            lock.writeLock().unlock();
        }
    }
    public UUID getPlayerId() { return playerId; }
    public double getBuffer() {
        lock.readLock().lock();
        try {
            return buffer;
        } finally {
            lock.readLock().unlock();
        }
    }
    public double getLastProbability() { return lastProbability; }
    public AimProcessor getAimProcessor() { return aimProcessor; }
}