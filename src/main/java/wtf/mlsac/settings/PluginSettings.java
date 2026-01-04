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

package wtf.mlsac.settings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public class PluginSettings {
    
    private final boolean debug;
    private final String dataDirectory;
    
    private final boolean detectionEnabled;
    private final String apiEndpoint;
    private final String apiKey;
    private final int sampleSize;
    private final int sampleInterval;
    
    private final double alertThreshold;
    private final boolean consoleAlerts;
    
    private final double vlThreshold;
    private final double vlResetValue;
    private final double vlMultiplier;
    private final double vlDecay;
    
    private final double penaltyMinProbability;
    private final Map<Integer, String> penaltyCommands;
    private final String customAlertPrefix;
    
    private final String messagePrefix;
    private final Map<String, String> messages;
    
    private final boolean autostartEnabled;
    private final String autostartLabel;
    private final String autostartComment;
    
    public static final boolean DEFAULT_DEBUG = false;
    public static final String DEFAULT_DATA_DIR = "plugins/MLSAC/data";
    public static final boolean DEFAULT_DETECTION_ENABLED = false;
    public static final String DEFAULT_API_ENDPOINT = "https://api.mlsac.wtf";
    public static final String DEFAULT_API_KEY = "";
    public static final int DEFAULT_SAMPLE_SIZE = 40;
    public static final int DEFAULT_SAMPLE_INTERVAL = 10;
    public static final double DEFAULT_ALERT_THRESHOLD = 0.75;
    public static final boolean DEFAULT_CONSOLE_ALERTS = true;
    public static final double DEFAULT_VL_THRESHOLD = 3.0;
    public static final double DEFAULT_VL_RESET = 1.0;
    public static final double DEFAULT_VL_MULTIPLIER = 50.0;
    public static final double DEFAULT_VL_DECAY = 0.5;
    public static final double DEFAULT_PENALTY_MIN_PROB = 0.85;
    public static final String DEFAULT_CUSTOM_ALERT_PREFIX = "&6[MLSAC] &f";
    public static final String DEFAULT_MESSAGE_PREFIX = "&6[MLSAC] &r";

    public PluginSettings() {
        this.debug = DEFAULT_DEBUG;
        this.dataDirectory = DEFAULT_DATA_DIR;
        this.detectionEnabled = DEFAULT_DETECTION_ENABLED;
        this.apiEndpoint = DEFAULT_API_ENDPOINT;
        this.apiKey = DEFAULT_API_KEY;
        this.sampleSize = DEFAULT_SAMPLE_SIZE;
        this.sampleInterval = DEFAULT_SAMPLE_INTERVAL;
        this.alertThreshold = DEFAULT_ALERT_THRESHOLD;
        this.consoleAlerts = DEFAULT_CONSOLE_ALERTS;
        this.vlThreshold = DEFAULT_VL_THRESHOLD;
        this.vlResetValue = DEFAULT_VL_RESET;
        this.vlMultiplier = DEFAULT_VL_MULTIPLIER;
        this.vlDecay = DEFAULT_VL_DECAY;
        this.penaltyMinProbability = DEFAULT_PENALTY_MIN_PROB;
        this.penaltyCommands = new HashMap<>();
        this.customAlertPrefix = DEFAULT_CUSTOM_ALERT_PREFIX;
        this.messagePrefix = DEFAULT_MESSAGE_PREFIX;
        this.messages = createDefaultMessages();
        this.autostartEnabled = false;
        this.autostartLabel = "UNLABELED";
        this.autostartComment = "";
    }
    
    public PluginSettings(JavaPlugin plugin) {
        this(plugin, null);
    }
    
    public PluginSettings(JavaPlugin plugin, Logger logger) {
        plugin.saveDefaultConfig();
        FileConfiguration cfg = plugin.getConfig();
        
        this.debug = cfg.getBoolean("debug", DEFAULT_DEBUG);
        this.dataDirectory = cfg.getString("outputDirectory", DEFAULT_DATA_DIR);
        
        this.detectionEnabled = cfg.getBoolean("detection.enabled", DEFAULT_DETECTION_ENABLED);
        this.apiEndpoint = cfg.getString("detection.endpoint", DEFAULT_API_ENDPOINT);
        this.apiKey = cfg.getString("detection.api-key", DEFAULT_API_KEY);
        this.sampleSize = cfg.getInt("detection.sample-size", DEFAULT_SAMPLE_SIZE);
        this.sampleInterval = cfg.getInt("detection.sample-interval", DEFAULT_SAMPLE_INTERVAL);
        
        double threshold = cfg.getDouble("alerts.threshold", DEFAULT_ALERT_THRESHOLD);
        this.alertThreshold = clampValue(threshold, 0.0, 1.0, "alerts.threshold", logger);
        this.consoleAlerts = cfg.getBoolean("alerts.console", DEFAULT_CONSOLE_ALERTS);
        
        this.vlThreshold = cfg.getDouble("violation.threshold", DEFAULT_VL_THRESHOLD);
        this.vlResetValue = cfg.getDouble("violation.reset-value", DEFAULT_VL_RESET);
        this.vlMultiplier = cfg.getDouble("violation.multiplier", DEFAULT_VL_MULTIPLIER);
        this.vlDecay = cfg.getDouble("violation.decay", DEFAULT_VL_DECAY);
        
        double minProb = cfg.getDouble("penalties.min-probability", DEFAULT_PENALTY_MIN_PROB);
        this.penaltyMinProbability = clampValue(minProb, 0.0, 1.0, "penalties.min-probability", logger);
        this.customAlertPrefix = cfg.getString("penalties.custom-alert-prefix", DEFAULT_CUSTOM_ALERT_PREFIX);

        this.penaltyCommands = new HashMap<>();
        ConfigurationSection cmdSection = cfg.getConfigurationSection("penalties.actions");
        if (cmdSection != null) {
            for (String key : cmdSection.getKeys(false)) {
                try {
                    int vl = Integer.parseInt(key);
                    String cmd = cmdSection.getString(key);
                    if (cmd != null && !cmd.isEmpty()) {
                        penaltyCommands.put(vl, cmd);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        this.messagePrefix = cfg.getString("messages.prefix", DEFAULT_MESSAGE_PREFIX);
        this.messages = createDefaultMessages();
        ConfigurationSection msgSection = cfg.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                if (!key.equals("prefix")) {
                    String msg = msgSection.getString(key);
                    if (msg != null) {
                        messages.put(key, msg);
                    }
                }
            }
        }
        
        this.autostartEnabled = cfg.getBoolean("autostart.enabled", false);
        this.autostartLabel = cfg.getString("autostart.label", "UNLABELED");
        this.autostartComment = cfg.getString("autostart.comment", "");
    }
    
    private double clampValue(double value, double min, double max, String path, Logger logger) {
        if (value < min || value > max) {
            double clamped = Math.max(min, Math.min(max, value));
            if (logger != null) {
                logger.warning("[Settings] " + path + " = " + value + 
                    " outside range [" + min + ", " + max + "], using " + clamped);
            }
            return clamped;
        }
        return value;
    }
    
    private static Map<String, String> createDefaultMessages() {
        Map<String, String> defaults = new HashMap<>();
        defaults.put("alerts-enabled", "&aAlerts enabled");
        defaults.put("alerts-disabled", "&eAlerts disabled");
        defaults.put("alert-format", "&c{PLAYER} &7| Prob: &e{PROBABILITY} &7| Buffer: &e{BUFFER}");
        defaults.put("alert-format-vl", "&c{PLAYER} &7| Prob: &e{PROBABILITY} &7| Buffer: &e{BUFFER} &7| VL: &c{VL}");
        return defaults;
    }

    public boolean isDebug() { return debug; }
    public String getDataDirectory() { return dataDirectory; }
    
    public boolean isDetectionEnabled() { return detectionEnabled; }
    public String getApiEndpoint() { return apiEndpoint; }
    public String getApiKey() { return apiKey; }
    public int getSampleSize() { return sampleSize; }
    public int getSampleInterval() { return sampleInterval; }
    
    public double getAlertThreshold() { return alertThreshold; }
    public boolean isConsoleAlerts() { return consoleAlerts; }
    
    public double getVlThreshold() { return vlThreshold; }
    public double getVlResetValue() { return vlResetValue; }
    public double getVlMultiplier() { return vlMultiplier; }
    public double getVlDecay() { return vlDecay; }
    
    public double getPenaltyMinProbability() { return penaltyMinProbability; }
    public Map<Integer, String> getPenaltyCommands() { return penaltyCommands; }
    public String getCustomAlertPrefix() { return customAlertPrefix; }
    
    public String getMessagePrefix() { return messagePrefix; }
    
    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }
    
    public String getMessage(String key, String player, double probability, double buffer, int vl) {
        String msg = getMessage(key);
        return msg
            .replace("{PLAYER}", player != null ? player : "")
            .replace("{PROBABILITY}", String.format("%.2f", probability))
            .replace("{BUFFER}", String.format("%.1f", buffer))
            .replace("{VL}", String.valueOf(vl))
            .replace("<player>", player != null ? player : "")
            .replace("<probability>", String.format("%.2f", probability))
            .replace("<buffer>", String.format("%.1f", buffer))
            .replace("<vl>", String.valueOf(vl));
    }
    
    public boolean isAutostartEnabled() { return autostartEnabled; }
    public String getAutostartLabel() { return autostartLabel; }
    public String getAutostartComment() { return autostartComment; }
}
