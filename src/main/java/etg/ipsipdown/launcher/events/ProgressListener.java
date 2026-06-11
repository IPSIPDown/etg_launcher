package etg.ipsipdown.launcher.events;

/**
 * Колбэк прогресса для сервисов. Сервисы ничего не знают про Swing —
 * UI сам решает, как показать статус и проценты.
 */
public interface ProgressListener {

    void onStatus(String text);

    void onProgress(int percent);
}
