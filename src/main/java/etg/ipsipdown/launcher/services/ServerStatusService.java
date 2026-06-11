package etg.ipsipdown.launcher.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import etg.ipsipdown.launcher.models.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Реализация протокола Server List Ping — то же самое, что делает
 * вкладка Multiplayer в самом Minecraft: онлайн, MOTD, версия, пинг.
 */
public class ServerStatusService {

    private static final Logger log = LoggerFactory.getLogger(ServerStatusService.class);

    private static final int TIMEOUT_MS = 5000;

    public static ServerStatus ping(String host, int port) {
        ServerStatus status = new ServerStatus();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // --- Handshake (state = status) ---
            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            handshake.writeByte(0x00);            // packet id
            writeVarInt(handshake, -1);           // protocol version (-1 = только статус)
            writeString(handshake, host);
            handshake.writeShort(port);
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
            log.info("Сервер {}:{} офлайн или недоступен: {}", host, port, e.getMessage());
            status.online = false;
        }
        return status;
    }

    private static void parseStatusJson(String json, ServerStatus status) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        if (root.has("players")) {
            JsonObject players = root.getAsJsonObject("players");
            if (players.has("online")) status.playersOnline = players.get("online").getAsInt();
            if (players.has("max")) status.playersMax = players.get("max").getAsInt();
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
