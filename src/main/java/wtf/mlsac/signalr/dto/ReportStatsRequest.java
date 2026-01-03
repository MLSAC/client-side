package wtf.mlsac.signalr.dto;

public class ReportStatsRequest {
    public int onlinePlayers;
    
    public ReportStatsRequest() {}
    
    public ReportStatsRequest(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }
}
