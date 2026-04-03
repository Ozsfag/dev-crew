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
    taskQueryService.getById(taskId);
    taskCommandService.updateStatus(taskId, TaskStatus.IN_PROGRESS);

    var sample = Timer.start(meterRegistry);
    String result;
    try {
      result = agentDispatcher.dispatch(role, prompt);
    } catch (Exception e) {
      if (rateLimitPolicy.isRateLimit(e)) {
        var retryAt = rateLimitPolicy.retryAt(timeProvider.now());
        log.warn("Rate limit от LLM для задачи {} (агент {}), повтор в {}", taskId, role, retryAt);
        taskCommandService.rateLimited(taskId, retryAt);
        recordTaskCounter(role, "RATE_LIMITED");
      } else {
        log.error("Агент {} упал при выполнении задачи {}", role, taskId, e);
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

  private void notifyHooks(UUID taskId, AgentRole role, String result) {
    postAgentHooks.forEach(hook -> hook.onAgentCompleted(taskId, role, result));
  }
}
