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

package wtf.mlsac.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.logging.Logger;

public class Config {
    private final boolean debug;
    private final int preHitTicks;
    private final int postHitTicks;
    private final double hitLockThreshold;
    private final int postHitTimeoutTicks;
    private final String outputDirectory;
    private final boolean aiEnabled;
    private final String aiApiKey;
    private final double aiAlertThreshold;
    private final boolean aiConsoleAlerts;
    private final double aiBufferFlag;
    private final double aiBufferResetOnFlag;
    private final double aiBufferMultiplier;
    private final double aiBufferDecrease;
    private final int aiSequence;
    private final int aiStep;
    private final double aiPunishmentMinProbability;
    private final Map<Integer, String> punishmentCommands;
    private final boolean animationEnabled;

    private final boolean liteBansEnabled;
    private final String liteBansDbHost;
    private final int liteBansDbPort;
    private final String liteBansDbName;
    private final String liteBansDbUsername;
    private final String liteBansDbPassword;
    private final String liteBansTablePrefix;
    private final int liteBansLookbackDays;
    private final Set<String> liteBansCheatReasons;
    private final boolean autostartEnabled;
    private final String autostartLabel;
    private final String autostartComment;
    private final String serverAddress;
    private final int reportStatsIntervalSeconds;
    private final boolean vlDecayEnabled;
    private final int vlDecayIntervalSeconds;
    private final int vlDecayAmount;
    private final boolean worldGuardEnabled;
    private final List<String> worldGuardDisabledRegions;
    private final boolean foliaEnabled;
    private final int foliaThreadPoolSize;
    private final boolean foliaEntitySchedulerEnabled;
    private final boolean foliaRegionSchedulerEnabled;
    private final Map<String, String> modelNames;
    private final Map<String, Boolean> modelOnlyAlert;
    private final boolean analyticsEnabled;
    private final int analyticsMinDetections;
    private final int analyticsColorGreenMax;
    private final int analyticsColorOrangeMax;
    public static final boolean DEFAULT_DEBUG = false;
    public static final String DEFAULT_OUTPUT_DIRECTORY = "plugins/MLSAC/data";
    public static final int PRE_HIT_TICKS = 5;
    public static final int POST_HIT_TICKS = 3;
    public static final double HIT_LOCK_THRESHOLD = 5.0;
    public static final int POST_HIT_TIMEOUT_TICKS = 40;
    public static final boolean DEFAULT_AI_ENABLED = false;
    public static final String DEFAULT_AI_API_KEY = "";
    public static final double DEFAULT_AI_ALERT_THRESHOLD = 0.5;
    public static final boolean DEFAULT_AI_CONSOLE_ALERTS = true;
    public static final double DEFAULT_AI_BUFFER_FLAG = 50.0;
    public static final double DEFAULT_AI_BUFFER_RESET_ON_FLAG = 25.0;
    public static final double DEFAULT_AI_BUFFER_MULTIPLIER = 100.0;
    public static final double DEFAULT_AI_BUFFER_DECREASE = 0.25;
    public static final double DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY = 0.85;
    public static final boolean DEFAULT_ANIMATION_ENABLED = true;
    public static final int DEFAULT_AI_SEQUENCE = 40;
    public static final int DEFAULT_AI_STEP = 10;

    public static final boolean DEFAULT_LITEBANS_ENABLED = false;
    public static final String DEFAULT_LITEBANS_DB_HOST = "localhost";
    public static final int DEFAULT_LITEBANS_DB_PORT = 3306;
    public static final String DEFAULT_LITEBANS_DB_NAME = "litebans";
    public static final String DEFAULT_LITEBANS_DB_USERNAME = "";
    public static final String DEFAULT_LITEBANS_DB_PASSWORD = "";
    public static final String DEFAULT_LITEBANS_TABLE_PREFIX = "litebans_";
    public static final int DEFAULT_LITEBANS_LOOKBACK_DAYS = 7;
    public static final boolean DEFAULT_AUTOSTART_ENABLED = false;
    public static final String DEFAULT_AUTOSTART_LABEL = "UNLABELED";
    public static final String DEFAULT_AUTOSTART_COMMENT = "";
    public static final String DEFAULT_SERVER_ADDRESS = "https://api.mlsac.wtf";
    public static final int DEFAULT_REPORT_STATS_INTERVAL_SECONDS = 30;
    public static final boolean DEFAULT_VL_DECAY_ENABLED = true;
    public static final int DEFAULT_VL_DECAY_INTERVAL_SECONDS = 60;
    public static final int DEFAULT_VL_DECAY_AMOUNT = 1;
    public static final boolean DEFAULT_WORLDGUARD_ENABLED = true;
    public static final List<String> DEFAULT_WORLDGUARD_DISABLED_REGIONS = new ArrayList<>();
    public static final boolean DEFAULT_FOLIA_ENABLED = true;
    public static final int DEFAULT_FOLIA_THREAD_POOL_SIZE = 0;
    public static final boolean DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED = true;
    public static final boolean DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED = true;
    public static final boolean DEFAULT_ANALYTICS_ENABLED = true;
    public static final int DEFAULT_ANALYTICS_MIN_DETECTIONS = 5;
    public static final int DEFAULT_ANALYTICS_COLOR_GREEN_MAX = 10;
    public static final int DEFAULT_ANALYTICS_COLOR_ORANGE_MAX = 20;

    public Config() {
        this.debug = DEFAULT_DEBUG;
        this.preHitTicks = PRE_HIT_TICKS;
        this.postHitTicks = POST_HIT_TICKS;
        this.hitLockThreshold = HIT_LOCK_THRESHOLD;
        this.postHitTimeoutTicks = POST_HIT_TIMEOUT_TICKS;
        this.outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        this.aiEnabled = DEFAULT_AI_ENABLED;
        this.aiApiKey = DEFAULT_AI_API_KEY;
        this.aiAlertThreshold = DEFAULT_AI_ALERT_THRESHOLD;
        this.aiConsoleAlerts = DEFAULT_AI_CONSOLE_ALERTS;
        this.aiBufferFlag = DEFAULT_AI_BUFFER_FLAG;
        this.aiBufferResetOnFlag = DEFAULT_AI_BUFFER_RESET_ON_FLAG;
        this.aiBufferMultiplier = DEFAULT_AI_BUFFER_MULTIPLIER;
        this.aiBufferDecrease = DEFAULT_AI_BUFFER_DECREASE;
        this.aiSequence = DEFAULT_AI_SEQUENCE;
        this.aiStep = DEFAULT_AI_STEP;
        this.aiPunishmentMinProbability = DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY;
        this.punishmentCommands = new HashMap<>();
        this.animationEnabled = DEFAULT_ANIMATION_ENABLED;

        this.liteBansEnabled = DEFAULT_LITEBANS_ENABLED;
        this.liteBansDbHost = DEFAULT_LITEBANS_DB_HOST;
        this.liteBansDbPort = DEFAULT_LITEBANS_DB_PORT;
        this.liteBansDbName = DEFAULT_LITEBANS_DB_NAME;
        this.liteBansDbUsername = DEFAULT_LITEBANS_DB_USERNAME;
        this.liteBansDbPassword = DEFAULT_LITEBANS_DB_PASSWORD;
        this.liteBansTablePrefix = DEFAULT_LITEBANS_TABLE_PREFIX;
        this.liteBansLookbackDays = DEFAULT_LITEBANS_LOOKBACK_DAYS;
        this.liteBansCheatReasons = createDefaultCheatReasons();
        this.autostartEnabled = DEFAULT_AUTOSTART_ENABLED;
        this.autostartLabel = DEFAULT_AUTOSTART_LABEL;
        this.autostartComment = DEFAULT_AUTOSTART_COMMENT;
        this.serverAddress = DEFAULT_SERVER_ADDRESS;
        this.reportStatsIntervalSeconds = DEFAULT_REPORT_STATS_INTERVAL_SECONDS;
        this.vlDecayEnabled = DEFAULT_VL_DECAY_ENABLED;
        this.vlDecayIntervalSeconds = DEFAULT_VL_DECAY_INTERVAL_SECONDS;
        this.vlDecayAmount = DEFAULT_VL_DECAY_AMOUNT;
        this.worldGuardEnabled = DEFAULT_WORLDGUARD_ENABLED;
        this.worldGuardDisabledRegions = new ArrayList<>(DEFAULT_WORLDGUARD_DISABLED_REGIONS);
        this.foliaEnabled = DEFAULT_FOLIA_ENABLED;
        this.foliaThreadPoolSize = DEFAULT_FOLIA_THREAD_POOL_SIZE;
        this.foliaEntitySchedulerEnabled = DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED;
        this.foliaRegionSchedulerEnabled = DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED;
        this.modelNames = new HashMap<>();
        this.modelOnlyAlert = new HashMap<>();
        this.analyticsEnabled = DEFAULT_ANALYTICS_ENABLED;
        this.analyticsMinDetections = DEFAULT_ANALYTICS_MIN_DETECTIONS;
        this.analyticsColorGreenMax = DEFAULT_ANALYTICS_COLOR_GREEN_MAX;
        this.analyticsColorOrangeMax = DEFAULT_ANALYTICS_COLOR_ORANGE_MAX;
    }

    private static Set<String> createDefaultCheatReasons() {
        Set<String> reasons = new HashSet<>();
        reasons.add("killaura");
        reasons.add("cheat");
        reasons.add("hack");
        return reasons;
    }

    public Config(JavaPlugin plugin) {
        this(plugin, null);
    }

    public Config(JavaPlugin plugin, Logger logger) {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        this.debug = config.getBoolean("debug", DEFAULT_DEBUG);
        this.preHitTicks = PRE_HIT_TICKS;
        this.postHitTicks = POST_HIT_TICKS;
        this.hitLockThreshold = HIT_LOCK_THRESHOLD;
        this.postHitTimeoutTicks = POST_HIT_TIMEOUT_TICKS;
        this.outputDirectory = config.getString("outputDirectory", DEFAULT_OUTPUT_DIRECTORY);
        this.aiEnabled = config.getBoolean("detection.enabled",
                config.getBoolean("ai.enabled", DEFAULT_AI_ENABLED));
        this.aiApiKey = config.getString("detection.api-key",
                config.getString("ai.api-key", DEFAULT_AI_API_KEY));
        double alertThreshold = config.getDouble("alerts.threshold",
                config.getDouble("ai.alert.threshold", DEFAULT_AI_ALERT_THRESHOLD));
        this.aiAlertThreshold = clampThreshold(alertThreshold, "alerts.threshold", logger);
        this.aiConsoleAlerts = config.getBoolean("alerts.console",
                config.getBoolean("ai.alert.console", DEFAULT_AI_CONSOLE_ALERTS));
        this.aiBufferFlag = config.getDouble("violation.threshold",
                config.getDouble("ai.buffer.flag", DEFAULT_AI_BUFFER_FLAG));
        this.aiBufferResetOnFlag = config.getDouble("violation.reset-value",
                config.getDouble("ai.buffer.reset-on-flag", DEFAULT_AI_BUFFER_RESET_ON_FLAG));
        this.aiBufferMultiplier = config.getDouble("violation.multiplier",
                config.getDouble("ai.buffer.multiplier", DEFAULT_AI_BUFFER_MULTIPLIER));
        this.aiBufferDecrease = config.getDouble("violation.decay",
                config.getDouble("ai.buffer.decrease", DEFAULT_AI_BUFFER_DECREASE));
        this.aiSequence = config.getInt("detection.sample-size",
                config.getInt("ai.sequence", DEFAULT_AI_SEQUENCE));
        this.aiStep = config.getInt("detection.sample-interval",
                config.getInt("ai.step", DEFAULT_AI_STEP));
        double punishmentMinProb = config.getDouble("penalties.min-probability",
                config.getDouble("ai.punishment.min-probability", DEFAULT_AI_PUNISHMENT_MIN_PROBABILITY));
        this.aiPunishmentMinProbability = clampThreshold(punishmentMinProb, "penalties.min-probability", logger);
        this.animationEnabled = config.getBoolean("penalties.animation.enabled", DEFAULT_ANIMATION_ENABLED);
        this.punishmentCommands = new HashMap<>();
        ConfigurationSection cmdSection = config.getConfigurationSection("penalties.actions");
        if (cmdSection == null) {
            cmdSection = config.getConfigurationSection("ai.punishment.commands");
        }
        if (cmdSection != null) {
            for (String key : cmdSection.getKeys(false)) {
                try {
                    int vl = Integer.parseInt(key);
                    String cmd = cmdSection.getString(key);
                    if (cmd != null && !cmd.isEmpty()) {
                        punishmentCommands.put(vl, cmd);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        this.liteBansEnabled = config.getBoolean("litebans.enabled", DEFAULT_LITEBANS_ENABLED);
        this.liteBansDbHost = config.getString("litebans.database.host", DEFAULT_LITEBANS_DB_HOST);
        this.liteBansDbPort = config.getInt("litebans.database.port", DEFAULT_LITEBANS_DB_PORT);
        this.liteBansDbName = config.getString("litebans.database.name", DEFAULT_LITEBANS_DB_NAME);
        this.liteBansDbUsername = config.getString("litebans.database.username", DEFAULT_LITEBANS_DB_USERNAME);
        this.liteBansDbPassword = config.getString("litebans.database.password", DEFAULT_LITEBANS_DB_PASSWORD);
        this.liteBansTablePrefix = config.getString("litebans.table-prefix", DEFAULT_LITEBANS_TABLE_PREFIX);
        this.liteBansLookbackDays = config.getInt("litebans.lookback-days", DEFAULT_LITEBANS_LOOKBACK_DAYS);
        this.liteBansCheatReasons = new HashSet<>();

        List<String> reasonsList = config.getStringList("litebans.cheat-reasons");
        if (reasonsList.isEmpty()) {
            this.liteBansCheatReasons.addAll(createDefaultCheatReasons());
        } else {
            this.liteBansCheatReasons.addAll(reasonsList);
        }

        this.autostartEnabled = config.getBoolean("autostart.enabled", DEFAULT_AUTOSTART_ENABLED);
        this.autostartLabel = config.getString("autostart.label", DEFAULT_AUTOSTART_LABEL);
        this.autostartComment = config.getString("autostart.comment", DEFAULT_AUTOSTART_COMMENT);
        this.serverAddress = config.getString("detection.endpoint",
                config.getString("ai.server", DEFAULT_SERVER_ADDRESS));
        this.reportStatsIntervalSeconds = DEFAULT_REPORT_STATS_INTERVAL_SECONDS;
        this.vlDecayEnabled = config.getBoolean("violation.vl-decay.enabled", DEFAULT_VL_DECAY_ENABLED);
        this.vlDecayIntervalSeconds = config.getInt("violation.vl-decay.interval", DEFAULT_VL_DECAY_INTERVAL_SECONDS);
        this.vlDecayAmount = config.getInt("violation.vl-decay.amount", DEFAULT_VL_DECAY_AMOUNT);
        this.worldGuardEnabled = config.getBoolean("detection.worldguard.enabled", DEFAULT_WORLDGUARD_ENABLED);
        this.worldGuardDisabledRegions = config.getStringList("detection.worldguard.disabled-regions");
        this.foliaEnabled = config.getBoolean("folia.enabled", DEFAULT_FOLIA_ENABLED);
        this.foliaThreadPoolSize = config.getInt("folia.thread-pool-size", DEFAULT_FOLIA_THREAD_POOL_SIZE);
        this.foliaEntitySchedulerEnabled = config.getBoolean("folia.entity-scheduler.enabled",
                DEFAULT_FOLIA_ENTITY_SCHEDULER_ENABLED);
        this.foliaRegionSchedulerEnabled = config.getBoolean("folia.region-scheduler.enabled",
                DEFAULT_FOLIA_REGION_SCHEDULER_ENABLED);

        this.modelNames = new HashMap<>();
        this.modelOnlyAlert = new HashMap<>();
        ConfigurationSection modelsSection = config.getConfigurationSection("detection.models");
        if (modelsSection != null) {
            for (String modelKey : modelsSection.getKeys(false)) {
                ConfigurationSection modelSection = modelsSection.getConfigurationSection(modelKey);
                if (modelSection != null) {
                    String displayName = modelSection.getString("name", modelKey);
                    boolean onlyAlertForModel = modelSection.getBoolean("only-alert", false);
                    modelNames.put(modelKey, displayName);
                    modelOnlyAlert.put(modelKey, onlyAlertForModel);
                } else {
                    String displayName = modelsSection.getString(modelKey);
                    if (displayName != null && !displayName.isEmpty()) {
                        modelNames.put(modelKey, displayName);
                        modelOnlyAlert.put(modelKey, false);
                    }
                }
            }
        }

        this.analyticsEnabled = config.getBoolean("analytics.enabled", DEFAULT_ANALYTICS_ENABLED);
        this.analyticsMinDetections = config.getInt("analytics.min-detections", DEFAULT_ANALYTICS_MIN_DETECTIONS);
        this.analyticsColorGreenMax = config.getInt("analytics.colors.green", DEFAULT_ANALYTICS_COLOR_GREEN_MAX);
        this.analyticsColorOrangeMax = config.getInt("analytics.colors.orange", DEFAULT_ANALYTICS_COLOR_ORANGE_MAX);
    }

    private double clampThreshold(double value, String configPath, Logger logger) {
        if (value < 0.0 || value > 1.0) {
            double clamped = Math.max(0.0, Math.min(1.0, value));
            if (logger != null) {
                logger.warning("[Config] " + configPath + " value " + value +
                        " is outside valid range [0.0, 1.0], clamped to " + clamped);
            }
            return clamped;
        }
        return value;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getPreHitTicks() {
        return preHitTicks;
    }

    public int getPostHitTicks() {
        return postHitTicks;
    }

    public double getHitLockThreshold() {
        return hitLockThreshold;
    }

    public int getPostHitTimeoutTicks() {
        return postHitTimeoutTicks;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public String getAiApiKey() {
        return aiApiKey;
    }

    public double getAiAlertThreshold() {
        return aiAlertThreshold;
    }

    public boolean isAiConsoleAlerts() {
        return aiConsoleAlerts;
    }

    public double getAiBufferFlag() {
        return aiBufferFlag;
    }

    public double getAiBufferResetOnFlag() {
        return aiBufferResetOnFlag;
    }

    public double getAiBufferMultiplier() {
        return aiBufferMultiplier;
    }

    public double getAiBufferDecrease() {
        return aiBufferDecrease;
    }

    public int getAiSequence() {
        return aiSequence;
    }

    public int getAiStep() {
        return aiStep;
    }

    public double getAiPunishmentMinProbability() {
        return aiPunishmentMinProbability;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public String getPunishmentCommand(int vl) {
        return punishmentCommands.get(vl);
    }

    public Map<Integer, String> getPunishmentCommands() {
        return punishmentCommands;
    }

    public boolean isLiteBansEnabled() {
        return liteBansEnabled;
    }

    public String getLiteBansDbHost() {
        return liteBansDbHost;
    }

    public int getLiteBansDbPort() {
        return liteBansDbPort;
    }

    public String getLiteBansDbName() {
        return liteBansDbName;
    }

    public String getLiteBansDbUsername() {
        return liteBansDbUsername;
    }

    public String getLiteBansDbPassword() {
        return liteBansDbPassword;
    }

    public String getLiteBansTablePrefix() {
        return liteBansTablePrefix;
    }

    public int getLiteBansLookbackDays() {
        return liteBansLookbackDays;
    }

    public Set<String> getLiteBansCheatReasons() {
        return liteBansCheatReasons;
    }

    public boolean isAutostartEnabled() {
        return autostartEnabled;
    }

    public String getAutostartLabel() {
        return autostartLabel;
    }

    public String getAutostartComment() {
        return autostartComment;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getReportStatsIntervalSeconds() {
        return reportStatsIntervalSeconds;
    }

    public String getServerHost() {
        int colonIndex = serverAddress.lastIndexOf(':');
        if (colonIndex > 0) {
            return serverAddress.substring(0, colonIndex);
        }
        return serverAddress;
    }

    public int getServerPort() {
        int colonIndex = serverAddress.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < serverAddress.length() - 1) {
            try {
                return Integer.parseInt(serverAddress.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                return 5000;
            }
        }
        return 5000;
    }

    public boolean isVlDecayEnabled() {
        return vlDecayEnabled;
    }

    public int getVlDecayIntervalSeconds() {
        return vlDecayIntervalSeconds;
    }

    public int getVlDecayAmount() {
        return vlDecayAmount;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public List<String> getWorldGuardDisabledRegions() {
        return worldGuardDisabledRegions;
    }

    public boolean isFoliaEnabled() {
        return foliaEnabled;
    }

    public int getFoliaThreadPoolSize() {
        return foliaThreadPoolSize;
    }

    public boolean isFoliaEntitySchedulerEnabled() {
        return foliaEntitySchedulerEnabled;
    }

    public boolean isFoliaRegionSchedulerEnabled() {
        return foliaRegionSchedulerEnabled;
    }

    public boolean isOnlyAlertForModel(String modelKey) {
        if (modelKey == null) {
            return false;
        }
        return modelOnlyAlert.getOrDefault(modelKey, false);
    }

    public String getModelDisplayName(String modelKey) {
        if (modelKey == null) {
            return "Unknown";
        }
        return modelNames.getOrDefault(modelKey, modelKey);
    }

    public Map<String, String> getModelNames() {
        return modelNames;
    }

    public Map<String, Boolean> getModelOnlyAlert() {
        return modelOnlyAlert;
    }

    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }

    public int getAnalyticsMinDetections() {
        return analyticsMinDetections;
    }

    public int getAnalyticsColorGreenMax() {
        return analyticsColorGreenMax;
    }

    public int getAnalyticsColorOrangeMax() {
        return analyticsColorOrangeMax;
    }

    public String getDetectionColor(int detections) {
        if (detections <= analyticsColorGreenMax) {
            return "&a";
        } else if (detections <= analyticsColorOrangeMax) {
            return "&6";
        } else {
            return "&c";
        }
    }
}