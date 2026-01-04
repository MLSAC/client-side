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

package wtf.mlsac.server;

public class AIResponse {
    
    private final double probability;
    private final String error;
    
    public AIResponse(double probability) {
        this(probability, null);
    }
    
    public AIResponse(double probability, String error) {
        this.probability = probability;
        this.error = error;
    }
    
    public double getProbability() {
        return probability;
    }
    
    public String getError() {
        return error;
    }
    
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
    
    public static AIResponse fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        
        try {
            String trimmed = json.trim();
            
            int errorIndex = trimmed.indexOf("\"error\"");
            if (errorIndex != -1) {
                int colonIndex = trimmed.indexOf(':', errorIndex);
                if (colonIndex != -1) {
                    int start = trimmed.indexOf('"', colonIndex + 1);
                    if (start != -1) {
                        int end = trimmed.indexOf('"', start + 1);
                        if (end != -1) {
                            String errorMsg = trimmed.substring(start + 1, end);
                            return new AIResponse(0.0, errorMsg);
                        }
                    }
                }
            }
            
            int probIndex = trimmed.indexOf("\"probability\"");
            if (probIndex == -1) {
                return null;
            }
            
            int colonIndex = trimmed.indexOf(':', probIndex);
            if (colonIndex == -1) {
                return null;
            }
            
            int start = colonIndex + 1;
            while (start < trimmed.length() && Character.isWhitespace(trimmed.charAt(start))) {
                start++;
            }
            
            int end = start;
            while (end < trimmed.length()) {
                char c = trimmed.charAt(end);
                if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                    break;
                }
                end++;
            }
            
            String probStr = trimmed.substring(start, end);
            double probability = Double.parseDouble(probStr);
            return new AIResponse(probability);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public String toJson() {
        return "{\"probability\":" + probability + "}";
    }
    
    @Override
    public String toString() {
        return "AIResponse{probability=" + probability + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AIResponse that = (AIResponse) obj;
        return Double.compare(that.probability, probability) == 0;
    }
    
    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(probability);
        return (int) (temp ^ (temp >>> 32));
    }
}
