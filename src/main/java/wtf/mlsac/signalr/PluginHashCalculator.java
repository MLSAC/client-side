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


package wtf.mlsac.signalr;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
public class PluginHashCalculator {
    private static final int BUFFER_SIZE = 8192;
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    public static String calculateHash(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            return "";
        }
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            java.util.List<java.util.jar.JarEntry> entries = new java.util.ArrayList<>();
            java.util.Enumeration<java.util.jar.JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                entries.add(en.nextElement());
            }
            entries.sort(java.util.Comparator.comparing(java.util.jar.JarEntry::getName));
            byte[] buffer = new byte[BUFFER_SIZE];
            for (java.util.jar.JarEntry entry : entries) {
                if (entry.isDirectory()) continue;
                digest.update(entry.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                try (java.io.InputStream is = jar.getInputStream(entry)) {
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        digest.update(buffer, 0, bytesRead);
                    }
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            return "";
        }
    }
    public static File getPluginJarFile(JavaPlugin plugin) {
        if (plugin == null) {
            return null;
        }
        try {
            Class<?> pluginClass = plugin.getClass();
            java.net.URL location = pluginClass.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                File file = new File(location.toURI());
                if (file.exists() && file.getName().endsWith(".jar")) {
                    return file;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[SignalR] Failed to locate plugin JAR file", e);
        }
        return null;
    }
    public static String calculatePluginHash(JavaPlugin plugin) {
        File jarFile = getPluginJarFile(plugin);
        if (jarFile == null) {
            if (plugin != null) {
                plugin.getLogger().warning("[SignalR] Could not locate plugin JAR file for hash calculation");
            }
            return "";
        }
        String hash = calculateHash(jarFile);
        if (hash.isEmpty() && plugin != null) {
            plugin.getLogger().warning("[SignalR] Failed to calculate plugin JAR hash");
        }
        return hash;
    }
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}