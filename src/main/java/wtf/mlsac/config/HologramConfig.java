/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 */

package wtf.mlsac.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class HologramConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public HologramConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "holograms.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("holograms.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    public void save() {
        if (config == null || configFile == null)
            return;
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save holograms.yml!");
        }
    }

    public void reload() {
        load();
    }
}
