package etg.ipsipdown.launcher.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import etg.ipsipdown.launcher.models.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

/**
 * Реализация протокола Server List Ping — то же самое, что делает
 * вкладка Multiplayer в самом Minecraft: онлайн, MOTD, версия, пинг.
 */
public class ServerStatusService {

    private static final Logger log = LoggerFactory.getLogger(ServerStatusService.class);

    private static final int TIMEOUT_MS = 5000;

    // Протокол Minecraft 1.21.1 — NeoForge-серверы сбрасывают handshake с -1
    private static final int PROTOCOL_VERSION = 767;

    public static ServerStatus ping(String host, int port) {
        ServerStatus status = new ServerStatus();

        // Как и сам Minecraft: если порт стандартный, сначала ищем SRV-запись
        // _minecraft._tcp.<host> — хостинги вроде playit.gg публикуют реальный порт там
        InetSocketAddress address = resolveAddress(host, port);

        try (Socket socket = new Socket()) {
            socket.connect(address, TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // --- Handshake (state = status) ---
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            handshake.writeByte(0x00);            // packet id
            writeVarInt(handshake, PROTOCOL_VERSION);
            writeString(handshake, address.getHostString());
            handshake.writeShort(address.getPort());
            writeVarInt(handshake, 1);            // next state: status
            writeVarInt(out, handshakeBytes.size());
            out.write(handshakeBytes.toByteArray());

            // --- Status request ---
            out.writeByte(0x01); // длина пакета
            out.writeByte(0x00); // packet id

            // --- Status response ---
            readVarInt(in);                 // длина пакета
            readVarInt(in);                 // packet id
            int jsonLength = readVarInt(in);
            byte[] jsonBytes = new byte[jsonLength];
            in.readFully(jsonBytes);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            // --- Ping/Pong для замера задержки ---
            long start = System.currentTimeMillis();
            out.writeByte(0x09);
            out.writeByte(0x01);
            out.writeLong(start);
            readVarInt(in);
            readVarInt(in);
            in.readLong();
            status.pingMs = System.currentTimeMillis() - start;

            parseStatusJson(json, status);
            status.online = true;
        } catch (Exception e) {
            log.info("Сервер {}:{} офлайн или недоступен: {}", address.getHostString(), address.getPort(), e.getMessage());
            status.online = false;
        }
        return status;
    }

    /**
     * Резолвит адрес так же, как клиент Minecraft: для стандартного порта 25565
     * сначала ищет SRV-запись _minecraft._tcp.<host> и берёт хост/порт из неё.
     */
    private static InetSocketAddress resolveAddress(String host, int port) {
        if (port != 25565) return new InetSocketAddress(host, port);
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            env.put(Context.PROVIDER_URL, "dns:");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");

            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes("_minecraft._tcp." + host, new String[]{"SRV"});
            Attribute srv = attrs.get("SRV");
            if (srv != null && srv.size() > 0) {
                // Формат записи: "priority weight port target"
                String[] parts = srv.get(0).toString().trim().split("\\s+");
                int srvPort = Integer.parseInt(parts[2]);
                String target = parts[3].endsWith(".") ? parts[3].substring(0, parts[3].length() - 1) : parts[3];
                log.info("SRV-запись для {}: {}:{}", host, target, srvPort);
                return new InetSocketAddress(target, srvPort);
            }
        } catch (Exception e) {
            log.debug("SRV-запись для {} не найдена: {}", host, e.getMessage());
        }
        return new InetSocketAddress(host, port);
    }

    private static void parseStatusJson(String json, ServerStatus status) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        if (root.has("players")) {
            JsonObject players = root.getAsJsonObject("players");
            if (players.has("online")) status.playersOnline = players.get("online").getAsInt();
            if (players.has("max")) status.playersMax = players.get("max").getAsInt();
            if (players.has("sample")) {
                for (JsonElement p : players.getAsJsonArray("sample")) {
                    JsonObject player = p.getAsJsonObject();
                    if (player.has("name")) status.players.add(player.get("name").getAsString());
                }
            }
        }
        if (root.has("version")) {
            JsonObject version = root.getAsJsonObject("version");
            if (version.has("name")) status.version = version.get("name").getAsString();
        }
        if (root.has("description")) {
            status.motd = flattenMotd(root.get("description")).replaceAll("§.", "").trim();
        }
    }

    /** MOTD может быть строкой или объектом {"text":..., "extra":[...]} — собираем весь текст. */
    private static String flattenMotd(JsonElement element) {
        if (element == null) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            StringBuilder sb = new StringBuilder();
            if (obj.has("text")) sb.append(obj.get("text").getAsString());
            if (obj.has("extra")) {
                JsonArray extra = obj.getAsJsonArray("extra");
                for (JsonElement child : extra) sb.append(flattenMotd(child));
            }
            return sb.toString();
        }
        return "";
    }

    // --- VarInt (формат протокола Minecraft) ---

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            byte b = in.readByte();
            value |= (b & 0x7F) << position;
            if ((b & 0x80) == 0) break;
            position += 7;
            if (position >= 32) throw new IOException("VarInt слишком длинный");
        }
        return value;
    }
}
