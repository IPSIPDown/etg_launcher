package etg.ipsipdown.launcher.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Итог синхронизации сборки — что изменилось (для changelog-уведомления).
 */
public class SyncResult {
    public final List<String> addedMods = new ArrayList<>();
    public final List<String> updatedMods = new ArrayList<>();
    public final List<String> removedMods = new ArrayList<>();
    public int totalDownloaded;

    public boolean hasModChanges() {
        return !addedMods.isEmpty() || !updatedMods.isEmpty() || !removedMods.isEmpty();
    }

    /** Короткая сводка для уведомления: «+3 новых, ↑5 обновлено, −2 удалено». */
    public String summary() {
        List<String> parts = new ArrayList<>();
        if (!addedMods.isEmpty()) parts.add("+" + addedMods.size() + " новых");
        if (!updatedMods.isEmpty()) parts.add("↑" + updatedMods.size() + " обновлено");
        if (!removedMods.isEmpty()) parts.add("−" + removedMods.size() + " удалено");
        return String.join(", ", parts);
    }
}
