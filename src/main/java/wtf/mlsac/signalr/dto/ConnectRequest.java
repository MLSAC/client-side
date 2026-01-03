package wtf.mlsac.signalr.dto;

public class ConnectRequest {
    public String apiKey;
    public String pluginHash;
    
    public ConnectRequest() {}
    
    public ConnectRequest(String apiKey, String pluginHash) {
        this.apiKey = apiKey;
        this.pluginHash = pluginHash;
    }
}
