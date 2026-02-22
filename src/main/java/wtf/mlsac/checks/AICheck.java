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

package wtf.mlsac.checks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import wtf.mlsac.Main;
import wtf.mlsac.alert.AlertManager;
import wtf.mlsac.compat.WorldGuardCompat;
import wtf.mlsac.config.Config;
import wtf.mlsac.data.AIPlayerData;
import wtf.mlsac.data.TickData;
import wtf.mlsac.scheduler.SchedulerAdapter;
import wtf.mlsac.scheduler.SchedulerManager;
import wtf.mlsac.server.AIClientProvider;
import wtf.mlsac.server.AIResponse;
import wtf.mlsac.server.FlatBufferSerializer;
import wtf.mlsac.server.IAIClient;
import wtf.mlsac.violation.ViolationManager;
import wtf.mlsac.util.GeyserUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AICheck {
    private final Main plugin;
    private final AIClientProvider clientProvider;
    private final AlertManager alertManager;
    private final ViolationManager violationManager;
    private final Logger logger;
    private final SchedulerAdapter schedulerAdapter;
    private final Map<UUID, AIPlayerData> playerData;
    private Config config;
    private WorldGuardCompat worldGuardCompat;
    private int sequence;
    private int step;

    public AICheck(Main plugin, Config config,
            AIClientProvider clientProvider,
            AlertManager alertManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.config = config;
        this.clientProvider = clientProvider;
        this.alertManager = alertManager;
        this.violationManager = violationManager;
        this.logger = plugin.getLogger();
        this.schedulerAdapter = SchedulerManager.getAdapter();
        this.playerData = new ConcurrentHashMap<>();
        this.sequence = config.getAiSequence();
        this.step = config.getAiStep();
        this.worldGuardCompat = new WorldGuardCompat(
                plugin.getLogger(),
                config.isWorldGuardEnabled(),
                config.getWorldGuardDisabledRegions());
    }

    public void setConfig(Config config) {
        this.config = config;
        this.sequence = config.getAiSequence();
        this.step = config.getAiStep();
        this.worldGuardCompat = new WorldGuardCompat(
                plugin.getLogger(),
                config.isWorldGuardEnabled(),
                config.getWorldGuardDisabledRegions());
    }

    public void onAttack(Player player, Entity target) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!(target instanceof Player)) {
            return;
        }
        if (worldGuardCompat.shouldBypassAICheck(player)) {
            plugin.debug("[AI] Skipping attack for " + player.getName() + " - in disabled WorldGuard region");
            return;
        }
        if (!player.isValid()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = getOrCreatePlayerData(player);
            if (data.isBedrock()) {
                plugin.debug("[AI] Skipping attack for " + player.getName() + " - Bedrock player detected");
                return;
            }
            if (!data.isInCombat()) {
                data.clearBuffer();
                data.getAimProcessor().reset();
                plugin.debug("[AI] New combat started for " + player.getName() + ", cleared old data");
            }
            data.onAttack();
            plugin.debug("[AI] Attack registered for " + player.getName() +
                    ", buffer=" + data.getBufferSize() + "/" + sequence);
        });
    }

    public void onTeleport(Player player) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = playerData.get(player.getUniqueId());
            if (data != null) {
                data.onTeleport();
                plugin.debug("[AI] Teleport registered for " + player.getName() + ", resetting data");
            }
        });
    }

    public void onTick(Player player) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!isClientAvailable()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = getOrCreatePlayerData(player);
            if (data.isBedrock())
                return;
            data.incrementTicksSinceAttack();
            if (data.getTicksSinceAttack() > sequence) {
                if (!data.isPendingRequest() && data.getBufferSize() >= sequence) {
                    plugin.debug("[AI] Combat ended for " + player.getName() +
                            ", sending final buffer (" + data.getBufferSize() + " ticks)");
                    data.setPendingRequest(true);
                    sendDataToAI(player, data);
                }
                if (!data.isPendingRequest() && data.getTicksSinceAttack() > sequence * 2 && data.getBufferSize() > 0) {
                    data.clearBuffer();
                }
                data.resetStepCounter();
                return;
            }
        });
    }

    public void onRotationPacket(Player player, float yaw, float pitch) {
        if (!config.isAiEnabled()) {
            return;
        }
        if (!isClientAvailable()) {
            return;
        }
        if (!player.isValid()) {
            return;
        }
        schedulerAdapter.runEntitySync(player, () -> {
            AIPlayerData data = playerData.get(player.getUniqueId());
            if (data == null) {
                return;
            }
            if (!data.isInCombat()) {
                return;
            }
            if (worldGuardCompat.shouldBypassAICheck(player)) {
                plugin.debug("[AI] Skipping rotation for " + player.getName() + " - in disabled WorldGuard region");
                return;
            }
            if (data.isBedrock())
                return;
            data.processTick(yaw, pitch);
            data.incrementStepCounter();
            if (data.shouldSendData(step, sequence)) {
                data.setPendingRequest(true);
                sendDataToAI(player, data);
                data.resetStepCounter();
            }
        });
    }

    private void sendDataToAI(Player player, AIPlayerData data) {
        try {
            List<TickData> ticks = data.getTickBuffer();
            if (ticks.size() < sequence) {
                plugin.debug("[AI] Not enough ticks for " + player.getName() +
                        ": " + ticks.size() + "/" + sequence);
                data.setPendingRequest(false);
                return;
            }
            IAIClient client = clientProvider.get();
            if (client == null) {
                plugin.debug("[AI] Client unavailable (null) for " + player.getName());
                data.setPendingRequest(false);
                return;
            }
            plugin.debug("[AI] Sending " + ticks.size() + " ticks for " + player.getName() +
                    " (ticksSinceAttack=" + data.getTicksSinceAttack() + ")");
            if (config.isDebug()) {
                plugin.debug("[AI] === TICK BUFFER START ===");
                int i = 0;
                for (TickData tick : ticks) {
                    plugin.debug("[AI] Tick[" + i + "]: dYaw=" + String.format("%.4f", tick.deltaYaw) +
                            ", dPitch=" + String.format("%.4f", tick.deltaPitch) +
                            ", aYaw=" + String.format("%.4f", tick.accelYaw) +
                            ", aPitch=" + String.format("%.4f", tick.accelPitch) +
                            ", jYaw=" + String.format("%.4f", tick.jerkYaw) +
                            ", jPitch=" + String.format("%.4f", tick.jerkPitch) +
                            ", gcdYaw=" + String.format("%.4f", tick.gcdErrorYaw) +
                            ", gcdPitch=" + String.format("%.4f", tick.gcdErrorPitch));
                    i++;
                }
                plugin.debug("[AI] === TICK BUFFER END ===");
            }
            byte[] serialized = FlatBufferSerializer.serialize(ticks);
            final UUID playerUuid = player.getUniqueId();
            final String playerName = player.getName();
            client.predict(serialized, playerUuid.toString(), playerName)
                    .subscribe(response -> {
                        schedulerAdapter.runSync(() -> processResponse(playerUuid, playerName, data, response));
                    }, error -> {
                        handleError(playerName, data, error);
                    });
        } catch (Exception e) {
            plugin.getLogger().warning("[AI] Unexpected error in sendDataToAI: " + e.getMessage());
            e.printStackTrace();
            data.setPendingRequest(false);
        }
    }

    private boolean isClientAvailable() {
        return clientProvider != null && clientProvider.isAvailable();
    }

    private void processResponse(UUID playerUuid, String playerName, AIPlayerData data, AIResponse response) {
        schedulerAdapter.runSync(() -> {
            data.setPendingRequest(false);
            data.clearBuffer();
            if (response.getError() != null && response.getError().contains("INVALID_SEQUENCE")) {
                handleInvalidSequence(response.getError());
                return;
            }
            double probability = response.getProbability();
            String modelName = response.getModel();
            boolean isOnlyAlert = config.isOnlyAlertForModel(modelName);

            plugin.debug("[AI] Response for " + playerName + ": probability=" +
                    String.format("%.3f", probability) + ", model=" + modelName +
                    ", onlyAlert=" + isOnlyAlert);

            if (!isOnlyAlert) {
                data.updateBuffer(probability, config.getAiBufferMultiplier(),
                        config.getAiBufferDecrease(), config.getAiAlertThreshold());
            } else {
                plugin.debug("[AI] Only-alert mode for model " + modelName + ", skipping buffer/punishment");
            }

            if (alertManager.shouldAlert(probability)) {
                alertManager.sendAlert(playerName, probability, data.getBuffer(), modelName);
            }

            if (!isOnlyAlert && data.shouldFlag(config.getAiBufferFlag())) {
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    violationManager.handleFlag(player, probability, data.getBuffer());
                } else {
                    logger.warning("[AI] Player " + playerName + " went offline before punishment");
                }
                data.resetBuffer(config.getAiBufferResetOnFlag());
            }
        });

    }

    private void handleInvalidSequence(String error) {
        try {
            String[] parts = error.split(":");
            if (parts.length >= 2) {
                int newSequence = Integer.parseInt(parts[1].trim());
                if (newSequence > 0 && newSequence != this.sequence) {
                    logger.info("[AI] Updating sequence from " + this.sequence + " to " + newSequence);
                    this.sequence = newSequence;
                    for (AIPlayerData data : playerData.values()) {
                        data.clearBuffer();
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.warning("[AI] Failed to parse new sequence from error: " + error);
        }
    }

    private void handleError(String playerName, AIPlayerData data, Throwable error) {
        if (data != null) {
            data.setPendingRequest(false);
        }
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        logger.warning("[AI] Error for " + playerName + ": " + cause.getMessage());
    }

    private AIPlayerData getOrCreatePlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(),
                uuid -> {
                    AIPlayerData data = new AIPlayerData(uuid, sequence);
                    if (GeyserUtil.isBedrockPlayer(uuid)) {
                        data.setBedrock(true);
                        logger.info("[AI] Detected Bedrock player: " + player.getName() + " - bypassing checks");
                    }
                    return data;
                });
    }

    public AIPlayerData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }

    public void handlePlayerQuit(Player player) {
    }

    public void clearAll() {
        playerData.clear();
    }

    public int getSequence() {
        return sequence;
    }

    public int getStep() {
        return step;
    }

    public WorldGuardCompat getWorldGuardCompat() {
        return worldGuardCompat;
    }
}