package etg.ipsipdown.launcher.net;

import java.util.List;

public class Manifest {
    public List<ManifestFile> files;

    public static class ManifestFile {
        public String path; // Куда сохранить (например, "mods/jei-1.20.1.jar")
        public String url;  // Прямая ссылка на скачивание файла
        public String hash; // MD5 хеш файла
        public long size;   // Размер файла в байтах
    }
}