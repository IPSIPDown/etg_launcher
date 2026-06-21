package etg.ipsipdown.launcher.services;

import com.google.gson.JsonObject;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Discord Rich Presence без сторонних библиотек.
 * Windows: named pipe \\.\pipe\discord-ipc-{i}
 * Linux:   Unix domain socket в $XDG_RUNTIME_DIR/discord-ipc-{i} (и fallback-пути)
 */
public class DiscordRichPresence {

    private static final Logger log = LoggerFactory.getLogger(DiscordRichPresence.class);

    private static final String APP_ID = "1513444076517724181";

    public static final boolean COMPANION_MODE = false;

    private static RandomAccessFile windowsPipe;
    private static SocketChannel linuxSocket;

    private static boolean isConnected() {
        return OsPaths.isWindows() ? windowsPipe != null : linuxSocket != null;
    }

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
        if (OsPaths.isWindows()) {
            connectWindows();
        } else {
            connectLinux();
        }

        JsonObject handshake = new JsonObject();
        handshake.addProperty("v", 1);
        handshake.addProperty("client_id", APP_ID);
        writeFrame(0, handshake.toString());
        readFrame();
    }

    private static void connectWindows() throws Exception {
        Exception last = null;
        for (int i = 0; i < 10; i++) {
            try {
                windowsPipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                return;
            } catch (Exception e) {
                last = e;
            }
        }
        throw last != null ? last : new Exception("Discord не запущен");
    }

    private static void connectLinux() throws Exception {
        List<String> dirs = new ArrayList<>();

        // Первый приоритет: $XDG_RUNTIME_DIR — стандарт на systemd-системах
        String xdg = System.getenv("XDG_RUNTIME_DIR");
        if (xdg != null && !xdg.isBlank()) dirs.add(xdg);

        // Fallback через id -u -> /run/user/{uid}
        try {
            Process p = new ProcessBuilder("id", "-u").start();
            p.waitFor(2, TimeUnit.SECONDS);
            if (p.exitValue() == 0) {
                String uid = new String(p.getInputStream().readAllBytes()).trim();
                if (!uid.isBlank()) dirs.add("/run/user/" + uid);
            }
        } catch (Exception ignored) {}

        // Финальные fallback-пути (старые версии Discord, snap-пакеты)
        dirs.add(System.getProperty("java.io.tmpdir"));
        dirs.add("/tmp");

        Exception last = null;
        for (String dir : dirs) {
            if (dir == null || dir.isBlank()) continue;
            for (int i = 0; i < 10; i++) {
                try {
                    Path socketPath = Path.of(dir, "discord-ipc-" + i);
                    linuxSocket = SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
                    linuxSocket.configureBlocking(true);
                    return;
                } catch (Exception e) {
                    last = e;
                }
            }
        }
        throw last != null ? last : new Exception("Discord не запущен");
    }

    public static void updateActivity(String details, String state) {
        if (!isConnected()) return;
        try {
            setActivity(details, state);
        } catch (Exception e) {
            log.info("Не удалось обновить Rich Presence: {}", e.getMessage());
        }
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
        byte[] bytes = buf.array();

        if (OsPaths.isWindows()) {
            windowsPipe.write(bytes);
        } else {
            ByteBuffer writeBuf = ByteBuffer.wrap(bytes);
            while (writeBuf.hasRemaining()) linuxSocket.write(writeBuf);
        }
    }

    private static void readFrame() throws Exception {
        byte[] header = new byte[8];
        readFully(header);
        int length = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (length > 0 && length < 65536) {
            byte[] body = new byte[length];
            readFully(body);
        }
    }

    private static void readFully(byte[] buf) throws Exception {
        if (OsPaths.isWindows()) {
            windowsPipe.readFully(buf);
        } else {
            ByteBuffer bb = ByteBuffer.wrap(buf);
            while (bb.hasRemaining()) {
                if (linuxSocket.read(bb) == -1) throw new Exception("Discord закрыл соединение");
            }
        }
    }
}
