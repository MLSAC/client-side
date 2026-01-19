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


package wtf.mlsac.violation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wtf.mlsac.Main;
import wtf.mlsac.alert.AlertManager;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.config.Config;
import wtf.mlsac.data.AIPlayerData;
import wtf.mlsac.penalty.ActionType;
import wtf.mlsac.penalty.PenaltyContext;
import wtf.mlsac.penalty.PenaltyExecutor;
import wtf.mlsac.scheduler.ScheduledTask;
import wtf.mlsac.scheduler.SchedulerManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
public class ViolationManager {
    private static final int MAX_KICK_HISTORY = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long PUNISHMENT_COOLDOWN_MS = 5000;
    private final Main plugin;
    private final AlertManager alertManager;
    private final Logger logger;
    private final Map<UUID, Integer> violationLevels;
    private final LinkedList<KickRecord> kickHistory;
    private final PenaltyExecutor penaltyExecutor;
    private final Map<UUID, Long> lastPunishmentTime;
    private Config config;
    private AICheck aiCheck;
    private ScheduledTask decayTask;
    public static class KickRecord {
        private final String playerName;
        private final double probability;
        private final double buffer;
        private final int vl;
        private final LocalDateTime time;
        private final String command;
        public KickRecord(String playerName, double probability, double buffer, int vl, String command) {
            this.playerName = playerName;
            this.probability = probability;
            this.buffer = buffer;
            this.vl = vl;
            this.time = LocalDateTime.now();
            this.command = command;
        }
        public String getPlayerName() { return playerName; }
        public double getProbability() { return probability; }
        public double getBuffer() { return buffer; }
        public int getVl() { return vl; }
        public LocalDateTime getTime() { return time; }
        public String getCommand() { return command; }
        public String getFormattedTime() {
            return time.format(TIME_FORMATTER);
        }
    }
    public ViolationManager(Main plugin, Config config, AlertManager alertManager) {
        this.plugin = plugin;
        this.config = config;
        this.alertManager = alertManager;
        this.logger = plugin.getLogger();
        this.violationLevels = new ConcurrentHashMap<>();
        this.kickHistory = new LinkedList<>();
        this.penaltyExecutor = new PenaltyExecutor(plugin);
        this.lastPunishmentTime = new ConcurrentHashMap<>();
        updatePenaltyExecutorConfig();
        startDecayTask();
    }
    public void setAICheck(AICheck aiCheck) {
        this.aiCheck = aiCheck;
    }
    private void startDecayTask() {
        stopDecayTask();
        if (!config.isVlDecayEnabled()) {
            return;
        }
        int intervalTicks = config.getVlDecayIntervalSeconds() * 20;
        decayTask = SchedulerManager.getAdapter().runSyncRepeating(this::processDecay, intervalTicks, intervalTicks);
        plugin.debug("[VL] Decay task started with interval " + config.getVlDecayIntervalSeconds() + "s");
    }
    private void stopDecayTask() {
        if (decayTask != null) {
            decayTask.cancel();
            decayTask = null;
        }
    }
    private void processDecay() {
        if (aiCheck == null) {
            return;
        }
        int decayAmount = config.getVlDecayAmount();
        for (Map.Entry<UUID, Integer> entry : violationLevels.entrySet()) {
            UUID playerId = entry.getKey();
            AIPlayerData playerData = aiCheck.getPlayerData(playerId);
            if (playerData != null && playerData.isInCombat()) {
                continue;
            }
            int oldVl = entry.getValue();
            int newVl = oldVl - decayAmount;
            if (newVl <= 0) {
                violationLevels.remove(playerId);
                plugin.debug("[VL] Decay: removed VL for " + playerId + " (was " + oldVl + ")");
            } else {
                violationLevels.put(playerId, newVl);
                plugin.debug("[VL] Decay: " + playerId + " VL " + oldVl + " -> " + newVl);
            }
        }
    }
    private void updatePenaltyExecutorConfig() {
        penaltyExecutor.setAlertPrefix(config.getCustomAlertPrefix());
        penaltyExecutor.setConsoleAlerts(config.isAiConsoleAlerts());
        penaltyExecutor.setAnimationEnabled(config.isAnimationEnabled());
    }
    public void setConfig(Config config) {
        this.config = config;
        updatePenaltyExecutorConfig();
        startDecayTask();
    }
    public void handleFlag(Player player, double probability, double buffer) {
        if (probability < config.getAiPunishmentMinProbability()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int newVl = incrementViolationLevel(uuid);
        alertManager.sendAlert(player.getName(), probability, buffer, newVl);
        plugin.debug("[AI] " + player.getName() + " flagged - VL: " + newVl + 
                   ", Prob: " + String.format("%.2f", probability) + 
                   ", Buffer: " + String.format("%.1f", buffer));
        String command = getApplicablePunishmentCommand(newVl);
        if (command != null) {
            ActionType actionType = ActionType.fromCommand(command);
            if (actionType.isPunishment()) {
                Long previousTime = lastPunishmentTime.get(uuid);
                if (previousTime != null && (now - previousTime) < PUNISHMENT_COOLDOWN_MS) {
                    plugin.debug("[AI] " + player.getName() + " punishment on cooldown, skipping " + actionType);
                    return;
                }
                lastPunishmentTime.put(uuid, now);
            }
            executeCommand(command, player, probability, buffer, newVl);
        }
    }
    public int incrementViolationLevel(UUID playerId) {
        return violationLevels.merge(playerId, 1, Integer::sum);
    }
    public int getViolationLevel(UUID playerId) {
        return violationLevels.getOrDefault(playerId, 0);
    }
    public void resetViolationLevel(UUID playerId) {
        violationLevels.remove(playerId);
    }
    public String getApplicablePunishmentCommand(int vl) {
        Map<Integer, String> commands = config.getPunishmentCommands();
        if (commands.isEmpty()) {
            return null;
        }
        if (commands.containsKey(vl)) {
            return commands.get(vl);
        }
        int maxThreshold = -1;
        int applicableThreshold = -1;
        for (int threshold : commands.keySet()) {
            if (threshold > maxThreshold) {
                maxThreshold = threshold;
            }
            if (threshold <= vl && threshold > applicableThreshold) {
                applicableThreshold = threshold;
            }
        }
        if (applicableThreshold == -1 && vl > maxThreshold) {
            return commands.get(maxThreshold);
        }
        return applicableThreshold > 0 ? commands.get(applicableThreshold) : null;
    }
    public void executeCommand(String command, Player player, double probability, double buffer, int vl) {
        PenaltyContext context = PenaltyContext.builder()
            .playerName(player.getName())
            .violationLevel(vl)
            .probability(probability)
            .buffer(buffer)
            .build();
        addKickRecord(new KickRecord(player.getName(), probability, buffer, vl, command));
        penaltyExecutor.execute(command, context);
    }
    private synchronized void addKickRecord(KickRecord record) {
        kickHistory.addFirst(record);
        while (kickHistory.size() > MAX_KICK_HISTORY) {
            kickHistory.removeLast();
        }
    }
    public synchronized List<KickRecord> getKickHistory() {
        return Collections.unmodifiableList(new ArrayList<>(kickHistory));
    }
    public PenaltyExecutor getPenaltyExecutor() {
        return penaltyExecutor;
    }
    public void handlePlayerQuit(Player player) {
        lastPunishmentTime.remove(player.getUniqueId());
    }
    public void decreaseViolationLevel(UUID playerId, int amount) {
        violationLevels.computeIfPresent(playerId, (k, v) -> {
            int newVl = v - amount;
            return newVl <= 0 ? null : newVl;
        });
    }
    public void clearAll() {
        violationLevels.clear();
        lastPunishmentTime.clear();
        synchronized (this) {
            kickHistory.clear();
        }
    }
    public void shutdown() {
        stopDecayTask();
        clearAll();
        penaltyExecutor.shutdown();
    }
}