package org.blacksoil.devcrew.task.app.service.command;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TaskCommandService {

  private final TaskStore taskStore;
  private final TimeProvider timeProvider;

  public TaskModel create(
      String title,
      String description,
      AgentRole assignedTo,
      UUID projectId,
      UUID orgId,
      UUID parentTaskId) {
    var now = timeProvider.now();
    var task =
        new TaskModel(
            UUID.randomUUID(),
            projectId,
            orgId,
            parentTaskId,
            title,
            description,
            assignedTo,
            TaskStatus.PENDING,
            null,
            null,
            now,
            now,
            null);
    log.info("Задача создана: id={}, role={}", task.id(), assignedTo);
    return taskStore.save(task);
  }

  public TaskModel updateStatus(UUID taskId, TaskStatus newStatus) {
    var task = findOrThrow(taskId);
    return taskStore.save(task.withStatus(newStatus).withUpdatedAt(timeProvider.now()));
  }

  public TaskModel complete(UUID taskId, String result) {
    var task = findOrThrow(taskId);
    log.info("Задача завершена: taskId={}", taskId);
    var summary = result != null ? result.substring(0, Math.min(2000, result.length())) : null;
    return taskStore.save(
        task.withStatus(TaskStatus.COMPLETED)
            .withResult(result)
            .withResultSummary(summary)
            .withRetryAt(null)
            .withUpdatedAt(timeProvider.now()));
  }

  public TaskModel fail(UUID taskId, String reason) {
    var task = findOrThrow(taskId);
    log.warn("Задача провалена: taskId={}, reason={}", taskId, reason);
    return taskStore.save(
        task.withStatus(TaskStatus.FAILED)
            .withResult(reason)
            .withRetryAt(null)
            .withUpdatedAt(timeProvider.now()));
  }

  public TaskModel rateLimited(UUID taskId, Instant retryAt) {
    var task = findOrThrow(taskId);
    log.warn("Задача в rate-limit: taskId={}, retryAt={}", taskId, retryAt);
    return taskStore.save(
        task.withStatus(TaskStatus.RATE_LIMITED)
            .withRetryAt(retryAt)
            .withUpdatedAt(timeProvider.now()));
  }

  private TaskModel findOrThrow(UUID taskId) {
    return taskStore.findById(taskId).orElseThrow(() -> new NotFoundException("Task", taskId));
  }
}
