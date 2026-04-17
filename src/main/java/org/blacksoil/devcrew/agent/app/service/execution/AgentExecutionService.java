package org.blacksoil.devcrew.agent.app.service.execution;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.app.policy.RateLimitPolicy;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.hook.PostAgentHook;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.slf4j.MDC;
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

  private final AgentDispatcher agentDispatcher;
  private final TaskQueryService taskQueryService;
  private final TaskCommandService taskCommandService;
  private final List<PostAgentHook> postAgentHooks;
  private final MeterRegistry meterRegistry;
  private final RateLimitPolicy rateLimitPolicy;
  private final TimeProvider timeProvider;

  @Async("agentExecutor")
  public void execute(UUID taskId, AgentRole role, String prompt) {
    MDC.put("taskId", taskId.toString());
    MDC.put("agentRole", role.name());
    try {
      executeInternal(taskId, role, prompt);
    } finally {
      MDC.clear();
    }
  }

  private void executeInternal(UUID taskId, AgentRole role, String prompt) {
    var task = taskQueryService.getById(taskId);
    taskCommandService.updateStatus(taskId, TaskStatus.IN_PROGRESS);

    var sample = Timer.start(meterRegistry);
    String result;
    try {
      result = agentDispatcher.dispatch(role, prompt);
    } catch (Exception e) {
      if (rateLimitPolicy.isRateLimit(e)) {
        var retryAt = rateLimitPolicy.retryAt(timeProvider.now());
        log.warn("Rate limit от LLM, повтор в {}", retryAt);
        taskCommandService.rateLimited(taskId, retryAt);
        recordTaskCounter(role, "RATE_LIMITED");
      } else {
        log.error("Агент упал при выполнении задачи", e);
        taskCommandService.fail(taskId, e.getMessage());
        recordTaskCounter(role, "FAILED");
      }
      return;
    }

    sample.stop(
        Timer.builder("devcrew.agent.duration")
            .description("Время выполнения агента")
            .tag("role", role.name())
            .register(meterRegistry));
    recordTaskCounter(role, "COMPLETED");
    log.info("Агент завершил задачу");
    taskCommandService.complete(taskId, result);
    notifyHooks(taskId, task.projectId(), task.orgId(), role, result);
  }

  private void recordTaskCounter(AgentRole role, String status) {
    Counter.builder("devcrew.task.total")
        .description("Количество задач по статусам и ролям")
        .tag("role", role.name())
        .tag("status", status)
        .register(meterRegistry)
        .increment();
  }

  private void notifyHooks(UUID taskId, UUID projectId, UUID orgId, AgentRole role, String result) {
    postAgentHooks.forEach(hook -> hook.onAgentCompleted(taskId, projectId, orgId, role, result));
  }
}
