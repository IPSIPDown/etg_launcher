package etg.ipsipdown.launcher.models;

import java.util.ArrayList;
import java.util.List;

public class ServerStatus {
    public boolean online;
    public int playersOnline;
    public int playersMax;
    public String motd = "";
    public String version = "";
    public long pingMs;
    /** Имена игроков онлайн (sample из ответа сервера, обычно до 12 имён). */
    public List<String> players = new ArrayList<>();

    public static ServerStatus offline() {
        return new ServerStatus();
    }
}
