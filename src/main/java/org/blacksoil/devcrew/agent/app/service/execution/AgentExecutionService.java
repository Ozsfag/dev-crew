package org.blacksoil.devcrew.agent.app.service.execution;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.BackendDevAgent;
import org.blacksoil.devcrew.agent.domain.CodeReviewAgent;
import org.blacksoil.devcrew.agent.domain.DevOpsAgent;
import org.blacksoil.devcrew.agent.domain.PostAgentHook;
import org.blacksoil.devcrew.agent.domain.QaAgent;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Запускает агента на задачу: переводит статус → вызывает агента → фиксирует результат → хуки.
 * Метод execute помечен @Async — выполняется в VirtualThread (см. AsyncConfig). Контроллер получает
 * 202 Accepted немедленно, клиент поллит GET /api/tasks/{id}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionService {

  private final BackendDevAgent backendDevAgent;
  private final QaAgent qaAgent;
  private final CodeReviewAgent codeReviewAgent;
  private final DevOpsAgent devOpsAgent;
  private final TaskQueryService taskQueryService;
  private final TaskCommandService taskCommandService;
  private final List<PostAgentHook> postAgentHooks;
  private final MeterRegistry meterRegistry;

  @Async("agentExecutor")
  public void execute(UUID taskId, AgentRole role, String prompt) {
    taskQueryService.getById(taskId);
    taskCommandService.updateStatus(taskId, TaskStatus.IN_PROGRESS);

    var sample = Timer.start(meterRegistry);
    String result;
    try {
      result = dispatchToAgent(role, prompt);
    } catch (Exception e) {
      log.error("Агент {} упал при выполнении задачи {}", role, taskId, e);
      taskCommandService.fail(taskId, e.getMessage());
      recordTaskCounter(role, "FAILED");
      return;
    }

    sample.stop(
        Timer.builder("devcrew.agent.duration")
            .description("Время выполнения агента")
            .tag("role", role.name())
            .register(meterRegistry));
    recordTaskCounter(role, "COMPLETED");
    taskCommandService.complete(taskId, result);
    notifyHooks(taskId, role, result);
  }

  private void recordTaskCounter(AgentRole role, String status) {
    Counter.builder("devcrew.task.total")
        .description("Количество задач по статусам и ролям")
        .tag("role", role.name())
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  private String dispatchToAgent(AgentRole role, String prompt) {
    return switch (role) {
      case BACKEND_DEV -> backendDevAgent.execute(prompt);
      case QA -> qaAgent.execute(prompt);
      case CODE_REVIEWER -> codeReviewAgent.execute(prompt);
      case DEVOPS -> devOpsAgent.execute(prompt);
      default -> throw new UnsupportedOperationException("Агент " + role + " ещё не реализован");
    };
  }

  private void notifyHooks(UUID taskId, AgentRole role, String result) {
    postAgentHooks.forEach(hook -> hook.onAgentCompleted(taskId, role, result));
  }
}
