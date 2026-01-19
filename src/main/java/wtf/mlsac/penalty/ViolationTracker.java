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


package wtf.mlsac.penalty;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.mlsac.alert.AlertManager;
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
public class ViolationTracker {
    private static final int MAX_PENALTY_HISTORY = 10;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final JavaPlugin plugin;
    private final Logger logger;
    private final PenaltyExecutor executor;
    private final AlertManager alertManager;
    private final Map<UUID, Integer> levels;
    private final LinkedList<PenaltyRecord> history;
    private double minProbability = 0.85;
    private Map<Integer, String> penaltyCommands = new ConcurrentHashMap<>();
    public static class PenaltyRecord {
        private final String playerName;
        private final ActionType actionType;
        private final int violationLevel;
        private final double probability;
        private final LocalDateTime timestamp;
        private final String command;
        public PenaltyRecord(String playerName, ActionType actionType, int vl, 
                            double probability, String command) {
            this.playerName = playerName;
            this.actionType = actionType;
            this.violationLevel = vl;
            this.probability = probability;
            this.timestamp = LocalDateTime.now();
            this.command = command;
        }
        public String getPlayerName() { return playerName; }
        public ActionType getActionType() { return actionType; }
        public int getViolationLevel() { return violationLevel; }
        public double getProbability() { return probability; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getCommand() { return command; }
        public String getFormattedTime() {
            return timestamp.format(TIME_FORMAT);
        }
    }
    public ViolationTracker(JavaPlugin plugin, AlertManager alertManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.alertManager = alertManager;
        this.executor = new PenaltyExecutor(plugin);
        this.levels = new ConcurrentHashMap<>();
        this.history = new LinkedList<>();
    }
    public void recordViolation(Player player, double probability, double buffer) {
        if (probability < minProbability) {
            return;
        }
        UUID uuid = player.getUniqueId();
        int newLevel = incrementLevel(uuid);
        alertManager.sendAlert(player.getName(), probability, buffer, newLevel);
        logger.info("[Penalty] " + player.getName() + " - VL: " + newLevel + 
                   ", Prob: " + String.format("%.2f", probability) + 
                   ", Buffer: " + String.format("%.1f", buffer));
        String command = findPenaltyCommand(newLevel);
        if (command != null) {
            executePenalty(command, player, probability, buffer, newLevel);
        }
    }
    public int incrementLevel(UUID playerId) {
        return levels.merge(playerId, 1, Integer::sum);
    }
    public int getLevel(UUID playerId) {
        return levels.getOrDefault(playerId, 0);
    }
    public void resetLevel(UUID playerId) {
        levels.remove(playerId);
    }
    public void decreaseLevel(UUID playerId, int amount) {
        levels.computeIfPresent(playerId, (k, v) -> {
            int newLevel = v - amount;
            return newLevel <= 0 ? null : newLevel;
        });
    }
    public String findPenaltyCommand(int vl) {
        if (penaltyCommands.isEmpty()) {
            return null;
        }
        if (penaltyCommands.containsKey(vl)) {
            return penaltyCommands.get(vl);
        }
        int maxThreshold = -1;
        int applicableThreshold = -1;
        for (int threshold : penaltyCommands.keySet()) {
            if (threshold > maxThreshold) {
                maxThreshold = threshold;
            }
            if (threshold <= vl && threshold > applicableThreshold) {
                applicableThreshold = threshold;
            }
        }
        if (applicableThreshold == -1 && vl > maxThreshold) {
            return penaltyCommands.get(maxThreshold);
        }
        return applicableThreshold > 0 ? penaltyCommands.get(applicableThreshold) : null;
    }
    private void executePenalty(String command, Player player, double probability, 
                                double buffer, int vl) {
        PenaltyContext context = PenaltyContext.builder()
            .playerName(player.getName())
            .violationLevel(vl)
            .probability(probability)
            .buffer(buffer)
            .build();
        ActionType type = ActionType.fromCommand(command);
        addToHistory(new PenaltyRecord(player.getName(), type, vl, probability, command));
        executor.execute(command, context);
    }
    private synchronized void addToHistory(PenaltyRecord record) {
        history.addFirst(record);
        while (history.size() > MAX_PENALTY_HISTORY) {
            history.removeLast();
        }
    }
    public synchronized List<PenaltyRecord> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }
    public void handlePlayerQuit(Player player) {
    }
    public void clearAll() {
        levels.clear();
        synchronized (this) {
            history.clear();
        }
    }
    public void setMinProbability(double minProb) {
        this.minProbability = minProb;
    }
    public void setPenaltyCommands(Map<Integer, String> commands) {
        this.penaltyCommands.clear();
        if (commands != null) {
            this.penaltyCommands.putAll(commands);
        }
    }
    public void setAlertPrefix(String prefix) {
        executor.setAlertPrefix(prefix);
    }
    public void setConsoleAlerts(boolean enabled) {
        executor.setConsoleAlerts(enabled);
    }
    public PenaltyExecutor getExecutor() {
        return executor;
    }
}