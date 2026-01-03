package wtf.mlsac.datacollector;

import wtf.mlsac.Main;
import wtf.mlsac.session.ISessionManager;

public class DataCollectorFactory {
    
    public static ISessionManager createSessionManager(Main plugin) {
        return new wtf.mlsac.session.SessionManager(plugin);
    }
}