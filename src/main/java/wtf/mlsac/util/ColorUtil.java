package wtf.mlsac.util;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);
        
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    public static String stripColors(String message) {
        if (message == null) {
            return null;
        }
        String stripped = HEX_PATTERN.matcher(message).replaceAll("");
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', stripped));
    }
}
