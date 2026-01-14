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

public enum ServerVersion {
    V1_16(16, 0),
    V1_16_5(16, 5),
    V1_17(17, 0),
    V1_18(18, 0),
    V1_19(19, 0),
    V1_20(20, 0),
    V1_20_5(20, 5),
    V1_21(21, 0),
    V1_21_1(21, 1),
    V1_21_4(21, 4),
    UNKNOWN(0, 0);

    private final int minor;
    private final int patch;

    ServerVersion(int minor, int patch) {
        this.minor = minor;
        this.patch = patch;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isAtLeast(ServerVersion other) {
        if (this == UNKNOWN || other == UNKNOWN) {
            return this == other;
        }
        if (this.minor != other.minor) {
            return this.minor > other.minor;
        }
        return this.patch >= other.patch;
    }

    public boolean isBelow(ServerVersion other) {
        if (this == UNKNOWN || other == UNKNOWN) {
            return false;
        }
        return !isAtLeast(other);
    }

    public boolean isBetween(ServerVersion min, ServerVersion max) {
        return isAtLeast(min) && isBelow(max);
    }

    public static ServerVersion fromString(String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            return UNKNOWN;
        }

        try {
            String version = versionString.split("-")[0];
            String[] parts = version.split("\\.");

            if (parts.length < 2) {
                return UNKNOWN;
            }

            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (major != 1) {
                return UNKNOWN;
            }

            return findBestMatch(minor, patch);
        } catch (NumberFormatException e) {
            return UNKNOWN;
        }
    }

    private static ServerVersion findBestMatch(int minor, int patch) {
        ServerVersion bestMatch = UNKNOWN;

        for (ServerVersion v : values()) {
            if (v == UNKNOWN) continue;

            if (v.minor == minor && v.patch <= patch) {
                // Same minor version, patch must be <= target
                if (v.patch == patch) {
                    return v; // Exact match
                }
                if (bestMatch == UNKNOWN || bestMatch.minor < minor || v.patch > bestMatch.patch) {
                    bestMatch = v;
                }
            } else if (v.minor < minor) {
                // Lower minor version - only use if we haven't found same minor match
                if (bestMatch == UNKNOWN || (bestMatch.minor < minor && v.minor > bestMatch.minor)) {
                    bestMatch = v;
                }
            }
        }

        return bestMatch;
    }
}
