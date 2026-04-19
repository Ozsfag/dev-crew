package org.blacksoil.devcrew.notification.adapter.out.telegram;

import java.util.Set;
import lombok.experimental.UtilityClass;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramFile;

@UtilityClass
class TelegramFileValidator {

  static final long MAX_FILE_SIZE = 20 * 1024 * 1024L;
  static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "md", "log", "json", "xml");

  static boolean isAllowed(TelegramFile file) {
    if (file.fileSize() != null && file.fileSize() > MAX_FILE_SIZE) {
      return false;
    }
    var ext = extension(file.filePath());
    return ALLOWED_EXTENSIONS.contains(ext);
  }

  private static String extension(String filePath) {
    if (filePath == null) return "";
    var dot = filePath.lastIndexOf('.');
    return dot >= 0 ? filePath.substring(dot + 1).toLowerCase() : "";
  }
}
