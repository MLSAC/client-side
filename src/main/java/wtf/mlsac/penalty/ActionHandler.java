package wtf.mlsac.penalty;

public interface ActionHandler {
    
    void handle(String command, PenaltyContext context);
    
    ActionType getActionType();
}
