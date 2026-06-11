package etg.ipsipdown.launcher.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import etg.ipsipdown.launcher.events.ProgressListener;
import etg.ipsipdown.launcher.models.Manifest;
import etg.ipsipdown.launcher.utils.HashUtil;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Синхронизация файлов сборки с manifest.json (бывший ModManager).
 * - SHA-256 каждого файла, с кэшем хэшей (size+mtime), чтобы не пересчитывать всё при каждом запуске;
 * - параллельное скачивание в несколько потоков;
 * - очистка устаревших модов с защитой пользовательских через custom_mods.txt.
 */
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private static final String BASE_URL = "https://pub-97e8a596b9e44332a4b339c99ee9ef01.r2.dev";
    private static final String MANIFEST_URL = BASE_URL + "/manifest.json";
    private static final int DOWNLOAD_THREADS = 4;

    private static final Path HASH_CACHE_FILE = OsPaths.GAME_DIR.resolve("hash-cache.json");

    private final ProgressListener progress;
    private final DownloadService downloader;
    private final Gson gson = new Gson();

    /** Запись кэша: если размер и время изменения файла совпали — хэш не пересчитываем. */
    private static class CachedHash {
        long size;
        long mtime;
        String hash;
    }

    public SyncService(ProgressListener progress, DownloadService downloader) {
        this.progress = progress;
        this.downloader = downloader;
    }

    public void syncFiles() throws Exception {
        progress.onStatus("Получение списка файлов...");
        progress.onProgress(0);

        Files.createDirectories(OsPaths.GAME_DIR);

        List<Manifest.ManifestFile> files = fetchManifest();
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("Лаунчер увидел 0 файлов!");
        }

        int totalFiles = files.size();
        progress.onStatus("Успешно прочитано: " + totalFiles + " файлов");
        log.info("Манифест: {} файлов", totalFiles);

        progress.onStatus("Очистка старых модов...");
        cleanObsoleteMods(files);

        Map<String, CachedHash> hashCache = loadHashCache();
        Map<String, CachedHash> newHashCache = new ConcurrentHashMap<>(hashCache);

        progress.onStatus("Проверка файлов...");
        List<Manifest.ManifestFile> toDownload = new ArrayList<>();
        int checked = 0;
        for (Manifest.ManifestFile fileInfo : files) {
            Path targetPath = resolveTarget(fileInfo);
            if (requiresDownload(targetPath, fileInfo.hash, newHashCache)) {
                toDownload.add(fileInfo);
            }
            checked++;
            progress.onProgress((int) (((double) checked / totalFiles) * 100));
        }

        if (toDownload.isEmpty()) {
            log.info("Все файлы актуальны, скачивать нечего.");
        } else {
            log.info("Требуется скачать {} файлов", toDownload.size());
            downloadAll(toDownload, newHashCache);
        }

        saveHashCache(newHashCache);
    }

    private List<Manifest.ManifestFile> fetchManifest() throws Exception {
        String jsonBody = downloader.fetchString(MANIFEST_URL).trim();
        if (jsonBody.startsWith("[")) {
            Type listType = new TypeToken<List<Manifest.ManifestFile>>() {}.getType();
            return gson.fromJson(jsonBody, listType);
        }
        Manifest manifest = gson.fromJson(jsonBody, Manifest.class);
        return manifest != null ? manifest.files : null;
    }

    private Path resolveTarget(Manifest.ManifestFile fileInfo) {
        String cleanPath = cleanPath(fileInfo.path);
        return (cleanPath.startsWith("versions") || cleanPath.startsWith("libraries"))
                ? OsPaths.MINECRAFT_DIR.resolve(cleanPath)
                : OsPaths.GAME_DIR.resolve(cleanPath);
    }

    private static String cleanPath(String path) {
        String cleanPath = path.replace("\\", "/");
        return cleanPath.startsWith("/") ? cleanPath.substring(1) : cleanPath;
    }

    private void downloadAll(List<Manifest.ManifestFile> toDownload, Map<String, CachedHash> hashCache) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(DOWNLOAD_THREADS);
        AtomicInteger done = new AtomicInteger();
        int total = toDownload.size();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Manifest.ManifestFile fileInfo : toDownload) {
                futures.add(pool.submit(() -> {
                    Path targetPath = resolveTarget(fileInfo);
                    try {
                        downloader.downloadFile(BASE_URL + "/" + cleanPath(fileInfo.path), targetPath);
                        cacheHash(hashCache, targetPath, fileInfo.hash);
                    } catch (Exception e) {
                        throw new RuntimeException("Не удалось скачать " + targetPath.getFileName() + ": " + e.getMessage(), e);
                    }
                    int current = done.incrementAndGet();
                    progress.onStatus("Скачивание (" + current + "/" + total + "): " + targetPath.getFileName());
                    progress.onProgress((int) (((double) current / total) * 100));
                    return null;
                }));
            }
            for (Future<?> f : futures) f.get(); // пробрасываем первую ошибку
        } finally {
            pool.shutdownNow();
        }
        progress.onStatus("Все файлы успешно обновлены!");
    }

    private void cleanObsoleteMods(List<Manifest.ManifestFile> manifestFiles) {
        try {
            Path modsDir = OsPaths.MODS_DIR;
            if (!Files.exists(modsDir)) return;

            Set<String> customModsWhitelist = new HashSet<>();
            if (Files.exists(OsPaths.CUSTOM_MODS_WHITELIST)) {
                customModsWhitelist.addAll(Files.readAllLines(OsPaths.CUSTOM_MODS_WHITELIST));
            }

            Set<Path> expectedMods = new HashSet<>();
            for (Manifest.ManifestFile fileInfo : manifestFiles) {
                String cleanPath = cleanPath(fileInfo.path);
                if (cleanPath.startsWith("mods/")) {
                    expectedMods.add(OsPaths.GAME_DIR.resolve(cleanPath).toAbsolutePath().normalize());
                }
            }

            try (Stream<Path> stream = Files.walk(modsDir)) {
                stream.filter(Files::isRegularFile).forEach(localFile -> {
                    Path normalizedLocal = localFile.toAbsolutePath().normalize();
                    String fileName = localFile.getFileName().toString();

                    if (!expectedMods.contains(normalizedLocal) && fileName.endsWith(".jar")) {
                        if (customModsWhitelist.contains(fileName)) {
                            log.info("[Защита] Кастомный мод игрока сохранен: {}", fileName);
                        } else {
                            try {
                                Files.delete(localFile);
                                log.info("[Очистка] Удален старый/лишний мод: {}", fileName);
                            } catch (Exception e) {
                                log.warn("Не удалось удалить файл: {}", fileName);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("Ошибка при очистке старых модов", e);
        }
    }

    private boolean requiresDownload(Path file, String expectedHash, Map<String, CachedHash> hashCache) {
        if (!Files.exists(file)) return true;
        try {
            long size = Files.size(file);
            long mtime = Files.getLastModifiedTime(file).toMillis();
            String key = file.toAbsolutePath().toString();

            CachedHash cached = hashCache.get(key);
            String actualHash;
            if (cached != null && cached.size == size && cached.mtime == mtime) {
                actualHash = cached.hash;
            } else {
                actualHash = HashUtil.sha256(file);
                CachedHash entry = new CachedHash();
                entry.size = size;
                entry.mtime = mtime;
                entry.hash = actualHash;
                hashCache.put(key, entry);
            }
            return !actualHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            return true;
        }
    }

    private void cacheHash(Map<String, CachedHash> hashCache, Path file, String hash) {
        try {
            CachedHash entry = new CachedHash();
            entry.size = Files.size(file);
            entry.mtime = Files.getLastModifiedTime(file).toMillis();
            entry.hash = hash;
            hashCache.put(file.toAbsolutePath().toString(), entry);
        } catch (Exception ignored) {
        }
    }

    private Map<String, CachedHash> loadHashCache() {
        try {
            if (Files.exists(HASH_CACHE_FILE)) {
                Type mapType = new TypeToken<HashMap<String, CachedHash>>() {}.getType();
                Map<String, CachedHash> cache = gson.fromJson(Files.readString(HASH_CACHE_FILE), mapType);
                if (cache != null) return cache;
            }
        } catch (Exception e) {
            log.warn("Кэш хэшей повреждён, пересоздаём: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    private void saveHashCache(Map<String, CachedHash> cache) {
        try {
            Files.writeString(HASH_CACHE_FILE, gson.toJson(cache));
        } catch (Exception e) {
            log.warn("Не удалось сохранить кэш хэшей: {}", e.getMessage());
        }
    }
}
