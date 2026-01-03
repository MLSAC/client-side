package wtf.mlsac.penalty;

public class ParsedAction {
    
    private final ActionType type;
    private final String command;
    
    public ParsedAction(ActionType type, String command) {
        this.type = type != null ? type : ActionType.RAW;
        this.command = command != null ? command : "";
    }
    
    public ActionType getType() {
        return type;
    }
    
    public String getCommand() {
        return command;
    }
    
    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }
    
    @Override
    public String toString() {
        return "ParsedAction{type=" + type + ", command='" + command + "'}";
    }
}
