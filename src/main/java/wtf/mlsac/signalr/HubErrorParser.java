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
            
            String code = obj.has("code") ? obj.get("code").getAsString() : INTERNAL_ERROR;
            String message = obj.has("message") ? obj.get("message").getAsString() : exceptionMessage;
            
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
