package org.blacksoil.devcrew.agent.app;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.app.service.execution.AgentExecutionService;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.check.PreRunCheck;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * Оркестратор агентов. Реализует двухфазную модель выполнения:
 *
 * <ol>
 *   <li>{@code submit} — создаёт задачу в БД и возвращает id (синхронно, 201 Created).
 *   <li>{@code run} — проверяет предусловия (PreRunCheck), запускает агента асинхронно (202
 *       Accepted). Клиент поллит GET /api/tasks/{id} для получения результата.
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class AgentOrchestratorImpl implements AgentOrchestrator {

  private final TaskCommandService taskCommandService;
  private final TaskQueryService taskQueryService;
  private final AgentExecutionService agentExecutionService;
  private final List<PreRunCheck> preRunChecks;

  /** Создаёт задачу в статусе PENDING. Не запускает агента — только регистрирует задачу. */
  @Override
  public UUID submit(
      String title,
      String description,
      AgentRole role,
      @Nullable UUID projectId,
      @Nullable UUID orgId) {
    var task = taskCommandService.create(title, description, role, projectId, orgId, null);
    return task.id();
  }

  /**
   * Запускает ранее созданную задачу. Последовательно: проверяет план/лимиты (PreRunCheck),
   * передаёт в AgentExecutionService для асинхронного выполнения.
   */
  @Override
  public void run(UUID taskId, AgentRole role) {
    var task = taskQueryService.getById(taskId);
    preRunChecks.forEach(check -> check.check(task.projectId()));
    agentExecutionService.execute(taskId, role, task.description());
  }
}
