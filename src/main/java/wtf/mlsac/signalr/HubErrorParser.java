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
public class HubErrorParser {
    private static final Gson GSON = new Gson();
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String NOT_AUTHENTICATED = "NOT_AUTHENTICATED";
    public static final String INVALID_DATA = "INVALID_DATA";
    public static final String STATS_REQUIRED = "STATS_REQUIRED";
    public static final String STATS_EXPIRED = "STATS_EXPIRED";
    public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static HubError parse(String exceptionMessage) {
        if (exceptionMessage == null || exceptionMessage.isEmpty()) {
            return new HubError(INTERNAL_ERROR, "Unknown error");
        }
        String json = extractJson(exceptionMessage);
        if (json == null) {
            return new HubError(INTERNAL_ERROR, exceptionMessage);
        }
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            String code = INTERNAL_ERROR;
            if (obj.has("code")) {
                code = obj.get("code").getAsString();
            } else if (obj.has("Code")) {
                code = obj.get("Code").getAsString();
            }
            String message = exceptionMessage;
            if (obj.has("message")) {
                message = obj.get("message").getAsString();
            } else if (obj.has("Message")) {
                message = obj.get("Message").getAsString();
            }
            return new HubError(code, message);
        } catch (JsonSyntaxException e) {
            return new HubError(INTERNAL_ERROR, exceptionMessage);
        }
    }
    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }
    public static boolean isRetryable(String code) {
        return STATS_REQUIRED.equals(code) || 
               STATS_EXPIRED.equals(code) || 
               NOT_AUTHENTICATED.equals(code);
    }
    public static boolean requiresReportStats(String code) {
        return STATS_REQUIRED.equals(code) || STATS_EXPIRED.equals(code);
    }
    public static boolean requiresReconnection(String code) {
        return NOT_AUTHENTICATED.equals(code);
    }
    public static class HubError {
        private final String code;
        private final String message;
        public HubError(String code, String message) {
            this.code = code;
            this.message = message;
        }
        public String getCode() {
            return code;
        }
        public String getMessage() {
            return message;
        }
        public String toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("code", code);
            obj.addProperty("message", message);
            return GSON.toJson(obj);
        }
        public static HubError fromJson(String json) {
            return parse(json);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            HubError other = (HubError) obj;
            return code.equals(other.code) && message.equals(other.message);
        }
        @Override
        public int hashCode() {
            return 31 * code.hashCode() + message.hashCode();
        }
        @Override
        public String toString() {
            return "HubError{code='" + code + "', message='" + message + "'}";
        }
    }
}