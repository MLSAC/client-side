/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package wtf.mlsac.compat;

import org.bukkit.Bukkit;

import java.util.logging.Logger;

public final class VersionAdapter {

    private static VersionAdapter instance;

    private final ServerVersion version;
    private final boolean isPaper;
    private final String rawVersion;
    private final Logger logger;
    private boolean debugEnabled = false;

    private VersionAdapter(Logger logger) {
        this.logger = logger;
        this.rawVersion = Bukkit.getBukkitVersion();
        this.version = detectVersion();
        this.isPaper = detectPaper();
    }

    // Package-private constructor for testing
    VersionAdapter(Logger logger, ServerVersion version, boolean isPaper) {
        this.logger = logger;
        this.version = version;
        this.isPaper = isPaper;
        this.rawVersion = "test";
    }

    public static void init(Logger logger) {
        if (instance == null) {
            instance = new VersionAdapter(logger);
        }
    }

    public static VersionAdapter get() {
        if (instance == null) {
            throw new IllegalStateException("VersionAdapter not initialized. Call init() first.");
        }
        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    static void reset() {
        instance = null;
    }

    public ServerVersion getVersion() {
        return version;
    }

    public boolean isPaper() {
        return isPaper;
    }

    public String getRawVersion() {
        return rawVersion;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isAtLeast(ServerVersion v) {
        return version.isAtLeast(v);
    }

    public boolean isBelow(ServerVersion v) {
        return version.isBelow(v);
    }

    public boolean isBetween(ServerVersion min, ServerVersion max) {
        return version.isBetween(min, max);
    }

    private ServerVersion detectVersion() {
        try {
            String bukkitVersion = Bukkit.getBukkitVersion();
            ServerVersion detected = ServerVersion.fromString(bukkitVersion);

            if (detected == ServerVersion.UNKNOWN) {
                if (logger != null) {
                    logger.warning("Could not detect server version from: " + bukkitVersion);
                    logger.warning("Defaulting to minimum compatibility mode (1.16.5)");
                }
                return ServerVersion.V1_16_5;
            }

            return detected;
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("Failed to detect server version: " + e.getMessage());
                logger.warning("Defaulting to minimum compatibility mode (1.16.5)");
            }
            return ServerVersion.V1_16_5;
        }
    }

    private boolean detectPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            // Try newer Paper detection
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    public void logCompatibilityInfo() {
        if (logger == null) return;

        logger.info("=== MLSAC Version Compatibility ===");
        logger.info("Server version: " + version + " (raw: " + rawVersion + ")");
        logger.info("Server type: " + (isPaper ? "Paper" : "Spigot/Bukkit"));
        logger.info("Compatibility mode: " + getCompatibilityMode());

        // Log active compatibility features
        if (isAtLeast(ServerVersion.V1_20_5)) {
            logger.info("Using modern particle/effect names (1.20.5+)");
        } else {
            logger.info("Using legacy particle/effect names (pre-1.20.5)");
        }

        if (!isPaper) {
            logger.info("Paper events not available - using scheduler fallbacks");
        }
    }

    public String getCompatibilityMode() {
        if (version.isAtLeast(ServerVersion.V1_20_5)) {
            return "Modern (1.20.5+)";
        } else if (version.isAtLeast(ServerVersion.V1_17)) {
            return "Legacy-Modern (1.17-1.20.4)";
        } else {
            return "Legacy (1.16.x)";
        }
    }
}
