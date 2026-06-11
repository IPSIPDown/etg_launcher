package etg.ipsipdown.launcher.services;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Discord Rich Presence («Играет в EternalSky») без сторонних библиотек —
 * минимальный клиент IPC-протокола Discord через named pipe.
 *
 * Чтобы включить: создай приложение на https://discord.com/developers/applications
 * (можно то же, где бот) и вставь его Application ID в APP_ID.
 * Пока APP_ID пустой — сервис просто ничего не делает.
 */
public class DiscordRichPresence {

    private static final Logger log = LoggerFactory.getLogger(DiscordRichPresence.class);

    /** Application ID из Discord Developer Portal. Пусто = выключено. */
    private static final String APP_ID = ""; // TODO: вставь Application ID своего Discord-приложения

    private static RandomAccessFile pipe;

    /** Подключиться и выставить статус. Все ошибки молча глотаются — это украшение, не критика. */
    public static void connectAsync() {
        if (APP_ID.isBlank()) return;
        Thread t = new Thread(() -> {
            try {
                connect();
                setActivity("В лаунчере", "Выбирает сборку");
                log.info("Discord Rich Presence подключен");
            } catch (Exception e) {
                log.info("Discord Rich Presence недоступен: {}", e.getMessage());
            }
        }, "discord-rpc");
        t.setDaemon(true);
        t.start();
    }

    private static void connect() throws Exception {
        Exception last = null;
        for (int i = 0; i < 10; i++) {
            try {
                pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                break;
            } catch (Exception e) {
                last = e;
            }
        }
        if (pipe == null) throw last != null ? last : new Exception("Discord не запущен");

        // Handshake (opcode 0)
        JsonObject handshake = new JsonObject();
        handshake.addProperty("v", 1);
        handshake.addProperty("client_id", APP_ID);
        writeFrame(0, handshake.toString());
        readFrame(); // ответ READY (или ошибка) — содержимое нам не нужно
    }

    private static void setActivity(String details, String state) throws Exception {
        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", System.currentTimeMillis());

        JsonObject activity = new JsonObject();
        activity.addProperty("details", details);
        activity.addProperty("state", state);
        activity.add("timestamps", timestamps);

        JsonObject args = new JsonObject();
        args.addProperty("pid", (int) ProcessHandle.current().pid());
        args.add("activity", activity);

        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.add("args", args);
        payload.addProperty("nonce", String.valueOf(System.nanoTime()));

        writeFrame(1, payload.toString());
    }

    private static void writeFrame(int opcode, String json) throws Exception {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(data.length);
        buf.put(data);
        pipe.write(buf.array());
    }

    private static void readFrame() throws Exception {
        byte[] header = new byte[8];
        pipe.readFully(header);
        int length = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (length > 0 && length < 65536) {
            byte[] body = new byte[length];
            pipe.readFully(body);
        }
    }
}
