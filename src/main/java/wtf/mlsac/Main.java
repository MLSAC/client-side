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

package wtf.mlsac;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import wtf.mlsac.alert.AlertManager;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.commands.CommandHandler;
import wtf.mlsac.config.Config;
import wtf.mlsac.config.Label;
import wtf.mlsac.datacollector.DataCollectorFactory;
import wtf.mlsac.listeners.HitListener;
import wtf.mlsac.listeners.PlayerListener;
import wtf.mlsac.listeners.RotationListener;
import wtf.mlsac.listeners.TeleportListener;
import wtf.mlsac.listeners.TickListener;
import wtf.mlsac.server.AIClientProvider;
import wtf.mlsac.session.ISessionManager;
import wtf.mlsac.session.SessionManager;
import wtf.mlsac.util.FeatureCalculator;
import wtf.mlsac.violation.ViolationManager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {

    private Config config;
    private ISessionManager sessionManager;
    private FeatureCalculator featureCalculator;
    private TickListener tickListener;
    private HitListener hitListener;
    private RotationListener rotationListener;
    private PlayerListener playerListener;
    private TeleportListener teleportListener;
    private CommandHandler commandHandler;
    
    private AIClientProvider aiClientProvider;
    private AlertManager alertManager;
    private ViolationManager violationManager;
    private AICheck aiCheck;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
            .reEncodeByDefault(false)
            .checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        
        saveDefaultConfig();
        this.config = new Config(this, getLogger());
        
        File outputDir = new File(config.getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        this.featureCalculator = new FeatureCalculator();
        this.sessionManager = DataCollectorFactory.createSessionManager(this);
        
        this.aiClientProvider = new AIClientProvider(this, config);
        this.alertManager = new AlertManager(this, config);
        this.violationManager = new ViolationManager(this, config, alertManager);
        this.aiCheck = new AICheck(this, config, aiClientProvider, alertManager, violationManager);
        this.violationManager.setAICheck(aiCheck);
        
        if (config.isAiEnabled()) {
            aiClientProvider.initialize().thenAccept(success -> {
                if (success) {
                    getLogger().info("SignalR: Connected to " + config.getServerAddress());
                } else {
                    getLogger().warning("SignalR: Failed to connect to InferenceServer");
                }
            });
        }
        
        this.tickListener = new TickListener(sessionManager, aiCheck);
        this.hitListener = new HitListener(sessionManager, aiCheck);
        this.rotationListener = new RotationListener(sessionManager, aiCheck);
        this.playerListener = new PlayerListener(this, aiCheck, alertManager, violationManager, 
            sessionManager instanceof SessionManager ? (SessionManager) sessionManager : null);
        this.teleportListener = new TeleportListener(aiCheck);
        
        this.tickListener.setHitListener(hitListener);
        
        getServer().getPluginManager().registerEvents(tickListener, this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        
        PacketEvents.getAPI().getEventManager().registerListener(hitListener);
        PacketEvents.getAPI().getEventManager().registerListener(rotationListener);
        
        this.commandHandler = new CommandHandler(sessionManager, alertManager, aiCheck, this);
        PluginCommand command = getCommand("mlsac");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }
        
        getLogger().info("MLSAC enabled successfully!");
        getLogger().info("Data collector: ENABLED (output: " + config.getOutputDirectory() + ")");
        
        if (config.isAiEnabled()) {
            getLogger().info("AI detection: ENABLED (threshold: " + config.getAiAlertThreshold() + ")");
        } else {
            getLogger().info("AI detection: DISABLED");
        }
        
    }
    
    @Override
    public void onDisable() {
        if (sessionManager != null) {
            getLogger().info("Stopping all active sessions...");
            sessionManager.stopAllSessions();
        }
        
        if (aiCheck != null) {
            aiCheck.clearAll();
        }
        if (violationManager != null) {
            violationManager.shutdown();
        }
        if (commandHandler != null) {
            commandHandler.cleanup();
        }
        
        if (aiClientProvider != null && aiClientProvider.isAvailable()) {
            getLogger().info("Shutting down SignalR client...");
            try {
                aiClientProvider.shutdown().get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                getLogger().warning("Error shutting down SignalR client: " + e.getMessage());
            }
        }
        
        PacketEvents.getAPI().terminate();
        
        getLogger().info("MLSAC disabled successfully!");
    }
    
    public void reloadPluginConfig() {
        reloadConfig();
        this.config = new Config(this, getLogger());
        
        alertManager.setConfig(config);
        violationManager.setConfig(config);
        aiCheck.setConfig(config);
        
        if (aiClientProvider != null) {
            aiClientProvider.setConfig(config);
            if (config.isAiEnabled()) {
                aiClientProvider.reload().thenAccept(success -> {
                    if (success) {
                        getLogger().info("SignalR: Reconnected to " + config.getServerAddress());
                    }
                });
            } else {
                aiClientProvider.shutdown();
            }
        }
        
        getLogger().info("Configuration reloaded!");
    }
    
    public Config getPluginConfig() {
        return config;
    }
    
    public ISessionManager getSessionManager() {
        return sessionManager;
    }
    
    public FeatureCalculator getFeatureCalculator() {
        return featureCalculator;
    }
    
    public AICheck getAiCheck() {
        return aiCheck;
    }
    
    public AlertManager getAlertManager() {
        return alertManager;
    }
    
    public ViolationManager getViolationManager() {
        return violationManager;
    }
    
    public AIClientProvider getAiClientProvider() {
        return aiClientProvider;
    }
    
    public void debug(String message) {
        if (config != null && config.isDebug()) {
            getLogger().info("[Debug] " + message);
        }
    }
}
