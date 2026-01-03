package wtf.mlsac.penalty;

public class ActionParser {
    
    public ParsedAction parse(String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) {
            return new ParsedAction(ActionType.RAW, "");
        }
        
        ActionType type = ActionType.fromCommand(rawCommand);
        String command = type.stripPrefix(rawCommand);
        
        return new ParsedAction(type, command);
    }
    
    public boolean hasActionPrefix(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }
        
        ActionType type = ActionType.fromCommand(command);
        return type != ActionType.RAW;
    }
}
