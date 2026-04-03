package org.blacksoil.devcrew.agent.domain;

import java.util.UUID;

/** Port-интерфейс оркестратора агентов. */
public interface AgentOrchestrator {

  /**
   * Принимает задачу, декомпозирует и запускает подходящего агента.
   *
   * @param title название задачи
   * @param description полное описание для агента
   * @param role роль агента, которому делегируется задача
   * @param projectId проект, в рамках которого создаётся задача (тенант-изоляция)
   * @return id созданной задачи
   */
  UUID submit(String title, String description, AgentRole role, UUID projectId);

  /** Запускает уже созданную задачу по id. */
  void run(UUID taskId, AgentRole role);
}
