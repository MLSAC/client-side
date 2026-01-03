package wtf.mlsac.penalty;

public class PenaltyContext {
    
    private final String playerName;
    private final int violationLevel;
    private final double probability;
    private final double buffer;
    
    public PenaltyContext(String playerName, int violationLevel, double probability, double buffer) {
        this.playerName = playerName != null ? playerName : "";
        this.violationLevel = violationLevel;
        this.probability = probability;
        this.buffer = buffer;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public int getViolationLevel() {
        return violationLevel;
    }
    
    public double getProbability() {
        return probability;
    }
    
    public double getBuffer() {
        return buffer;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String playerName = "";
        private int violationLevel = 0;
        private double probability = 0.0;
        private double buffer = 0.0;
        
        public Builder playerName(String name) {
            this.playerName = name;
            return this;
        }
        
        public Builder violationLevel(int vl) {
            this.violationLevel = vl;
            return this;
        }
        
        public Builder probability(double prob) {
            this.probability = prob;
            return this;
        }
        
        public Builder buffer(double buf) {
            this.buffer = buf;
            return this;
        }
        
        public PenaltyContext build() {
            return new PenaltyContext(playerName, violationLevel, probability, buffer);
        }
    }
}
