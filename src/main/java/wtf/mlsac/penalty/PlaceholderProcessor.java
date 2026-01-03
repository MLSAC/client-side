package wtf.mlsac.penalty;

public class PlaceholderProcessor {
    
    private static final String PH_PLAYER = "{PLAYER}";
    private static final String PH_VL = "{VL}";
    private static final String PH_PROBABILITY = "{PROBABILITY}";
    private static final String PH_BUFFER = "{BUFFER}";
    
    private static final String LEGACY_PLAYER = "<player>";
    private static final String LEGACY_VL = "<vl>";
    private static final String LEGACY_PROBABILITY = "<probability>";
    private static final String LEGACY_BUFFER = "<buffer>";
    
    public String process(String template, PenaltyContext context) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        
        if (context == null) {
            return template;
        }
        
        String result = template;
        
        result = result.replace(PH_PLAYER, context.getPlayerName());
        result = result.replace(PH_VL, String.valueOf(context.getViolationLevel()));
        result = result.replace(PH_PROBABILITY, formatDouble(context.getProbability()));
        result = result.replace(PH_BUFFER, formatDouble(context.getBuffer()));
        
        result = result.replace(LEGACY_PLAYER, context.getPlayerName());
        result = result.replace(LEGACY_VL, String.valueOf(context.getViolationLevel()));
        result = result.replace(LEGACY_PROBABILITY, formatDouble(context.getProbability()));
        result = result.replace(LEGACY_BUFFER, formatDouble(context.getBuffer()));
        
        return result;
    }
    
    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }
    
    public boolean hasPlaceholders(String template) {
        if (template == null || template.isEmpty()) {
            return false;
        }
        
        return template.contains(PH_PLAYER) || template.contains(PH_VL) ||
               template.contains(PH_PROBABILITY) || template.contains(PH_BUFFER) ||
               template.contains(LEGACY_PLAYER) || template.contains(LEGACY_VL) ||
               template.contains(LEGACY_PROBABILITY) || template.contains(LEGACY_BUFFER);
    }
}
