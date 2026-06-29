package org.blacksoil.devcrew.notification.app.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурация интерактивного режима Telegram (devcrew.interactive.*): карта «имя проекта → путь к
 * репозиторию», в котором запускается claude.
 */
@Data
@ConfigurationProperties(prefix = "devcrew.interactive")
public class InteractiveProperties {

  /** Доступные проекты: имя (для /project) → абсолютный путь к репозиторию. */
  private Map<String, String> projects = new LinkedHashMap<>();
}
