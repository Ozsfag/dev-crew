package org.blacksoil.devcrew.task.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Конфигурация задач через application.yml (devcrew.task.*). */
@Data
@ConfigurationProperties(prefix = "devcrew.task")
public class TaskProperties {

  /** Максимальное число подзадач у одной родительской задачи. */
  private int maxSubtasks = 10;
}
