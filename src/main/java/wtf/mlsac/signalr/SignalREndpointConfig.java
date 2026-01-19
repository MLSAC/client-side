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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
public class SignalREndpointConfig {
    private static final String DEFAULT_HUB = "/v1/beta";
    private static final String DEFAULT_TRANSPORT = "WebSockets";
    private static final Map<String, String> DEFAULT_METHODS = Map.of(
        "connect", "Connect",
        "heartbeat", "Heartbeat",
        "reportStats", "ReportStats",
        "predict", "Predict"
    );
    private final String hub;
    private final String transport;
    private final Map<String, String> methods;
    private SignalREndpointConfig(String hub, String transport, Map<String, String> methods) {
        this.hub = hub != null ? hub : DEFAULT_HUB;
        this.transport = transport != null ? transport : DEFAULT_TRANSPORT;
        this.methods = methods != null ? methods : new HashMap<>(DEFAULT_METHODS);
    }
    public static SignalREndpointConfig defaults() {
        return new SignalREndpointConfig(DEFAULT_HUB, DEFAULT_TRANSPORT, new HashMap<>(DEFAULT_METHODS));
    }
    public static SignalREndpointConfig fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return defaults();
        }
        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);
            String hub = root.has("hub") ? root.get("hub").getAsString() : DEFAULT_HUB;
            String transport = root.has("transport") ? root.get("transport").getAsString() : DEFAULT_TRANSPORT;
            Map<String, String> methods = new HashMap<>(DEFAULT_METHODS);
            if (root.has("methods") && root.get("methods").isJsonObject()) {
                JsonObject methodsObj = root.getAsJsonObject("methods");
                for (String key : methodsObj.entrySet().stream().map(e -> e.getKey()).toList()) {
                    methods.put(key, methodsObj.get(key).getAsString());
                }
            }
            return new SignalREndpointConfig(hub, transport, methods);
        } catch (JsonSyntaxException e) {
            return defaults();
        }
    }
    public String toJson() {
        Gson gson = new Gson();
        JsonObject root = new JsonObject();
        root.addProperty("hub", hub);
        root.addProperty("transport", transport);
        JsonObject methodsObj = new JsonObject();
        for (Map.Entry<String, String> entry : methods.entrySet()) {
            methodsObj.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("methods", methodsObj);
        return gson.toJson(root);
    }
    public String getHubUrl(String serverAddress) {
        String baseUrl = normalizeServerAddress(serverAddress);
        return baseUrl + hub;
    }
    public String getMethodName(String methodKey) {
        return methods.getOrDefault(methodKey, DEFAULT_METHODS.get(methodKey));
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
    public String getHub() {
        return hub;
    }
    public String getTransport() {
        return transport;
    }
    public Map<String, String> getMethods() {
        return new HashMap<>(methods);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SignalREndpointConfig other = (SignalREndpointConfig) obj;
        return hub.equals(other.hub) && 
               transport.equals(other.transport) && 
               methods.equals(other.methods);
    }
    @Override
    public int hashCode() {
        int result = hub.hashCode();
        result = 31 * result + transport.hashCode();
        result = 31 * result + methods.hashCode();
        return result;
    }
    @Override
    public String toString() {
        return "SignalREndpointConfig{hub='" + hub + "', transport='" + transport + "', methods=" + methods + "}";
    }
}