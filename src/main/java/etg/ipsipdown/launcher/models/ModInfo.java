package etg.ipsipdown.launcher.models;

import java.awt.Image;
import java.nio.file.Path;

/**
 * Данные одного установленного мода (читаются из .jar файла).
 */
public class ModInfo {
    public String fileName;      // Имя файла (например: jei-1.20.jar)
    public String displayName;   // Красивое имя (например: Just Enough Items)
    public String version;       // Версия мода
    public String updateDate;    // Дата загрузки/обновления файла
    public Image icon;           // Иконка (вытащенная из .jar)
    public boolean isEnabled;    // Включен или выключен
    public Path filePath;        // Полный путь к файлу на диске
    public String sideType;      // Тип мода (Клиентский/Серверный/Общий)
}
