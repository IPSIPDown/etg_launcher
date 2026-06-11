package etg.ipsipdown.launcher.models;

public class ServerStatus {
    public boolean online;
    public int playersOnline;
    public int playersMax;
    public String motd = "";
    public String version = "";
    public long pingMs;

    public static ServerStatus offline() {
        return new ServerStatus();
    }
}
