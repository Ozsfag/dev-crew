package org.blacksoil.devcrew.agent.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически проверяет задачи в статусе RATE_LIMITED и повторно запускает те, у которых retryAt
 * уже наступил. Позволяет приложению самостоятельно возобновить работу после снятия ограничения
 * Anthropic API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitRetryScheduler {

  private final TaskQueryService taskQueryService;
  private final AgentOrchestrator agentOrchestrator;
  private final TimeProvider timeProvider;

  @Scheduled(fixedDelayString = "${devcrew.agent.rate-limit.retry-check-delay:30000}")
  public void retryRateLimitedTasks() {
    var now = timeProvider.now();
    var tasks = taskQueryService.getRateLimitedReadyToRetry(now);
    if (tasks.isEmpty()) {
      return;
    }
    log.info("Повторный запуск {} задач после снятия rate-limit", tasks.size());
    tasks.forEach(
        task -> {
          log.debug("Повтор задачи {} (агент {})", task.id(), task.assignedTo());
          agentOrchestrator.run(task.id(), task.assignedTo());
        });
  }
}
