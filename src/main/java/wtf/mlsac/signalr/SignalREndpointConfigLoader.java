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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
public class SignalREndpointConfigLoader {
    private static final String CONFIG_PATH = "/api/config/signalr";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private final Logger logger;
    public SignalREndpointConfigLoader(Logger logger) {
        this.logger = logger;
    }
    public SignalREndpointConfig loadSync(String serverAddress) {
        try {
            String configUrl = buildConfigUrl(serverAddress);
            String json = fetchJson(configUrl);
            SignalREndpointConfig config = SignalREndpointConfig.fromJson(json);
            logger.info("[SignalR] Loaded endpoint config: hub=" + config.getHub());
            return config;
        } catch (Exception e) {
            logger.log(Level.WARNING, "[SignalR] Failed to load endpoint config, using defaults: " + e.getMessage());
            return SignalREndpointConfig.defaults();
        }
    }
    public CompletableFuture<SignalREndpointConfig> loadAsync(String serverAddress) {
        return CompletableFuture.supplyAsync(() -> loadSync(serverAddress));
    }
    private String buildConfigUrl(String serverAddress) {
        String baseUrl = normalizeServerAddress(serverAddress);
        return baseUrl + CONFIG_PATH;
    }
    private String normalizeServerAddress(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return "https://api.mlsac.wtf";
        }
        String address = serverAddress.trim();
        if (address.startsWith("http://") || address.startsWith("https://")) {
            return address.endsWith("/") ? address.substring(0, address.length() - 1) : address;
        }
        return "https://" + address;
    }
    private String fetchJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP error: " + responseCode);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            connection.disconnect();
        }
    }
}