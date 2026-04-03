package org.blacksoil.devcrew.task.app.service.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskCommandService {

  private final TaskStore taskStore;
  private final TimeProvider timeProvider;

  public TaskModel create(
      String title, String description, AgentRole assignedTo, UUID projectId, UUID parentTaskId) {
    var now = timeProvider.now();
    var task =
        new TaskModel(
            UUID.randomUUID(),
            projectId,
            parentTaskId,
            title,
            description,
            assignedTo,
            TaskStatus.PENDING,
            null,
            now,
            now,
            null);
    return taskStore.save(task);
  }

  public TaskModel updateStatus(UUID taskId, TaskStatus newStatus) {
    var task = findOrThrow(taskId);
    var updated =
        new TaskModel(
            task.id(),
            task.projectId(),
            task.parentTaskId(),
            task.title(),
            task.description(),
            task.assignedTo(),
            newStatus,
            task.result(),
            task.createdAt(),
            timeProvider.now(),
            task.retryAt());
    return taskStore.save(updated);
  }

  public TaskModel complete(UUID taskId, String result) {
    var task = findOrThrow(taskId);
    var completed =
        new TaskModel(
            task.id(),
            task.projectId(),
            task.parentTaskId(),
            task.title(),
            task.description(),
            task.assignedTo(),
            TaskStatus.COMPLETED,
            result,
            task.createdAt(),
            timeProvider.now(),
            null);
    return taskStore.save(completed);
  }

  public TaskModel fail(UUID taskId, String reason) {
    var task = findOrThrow(taskId);
    var failed =
        new TaskModel(
            task.id(),
            task.projectId(),
            task.parentTaskId(),
            task.title(),
            task.description(),
            task.assignedTo(),
            TaskStatus.FAILED,
            reason,
            task.createdAt(),
            timeProvider.now(),
            null);
    return taskStore.save(failed);
  }

  public TaskModel rateLimited(UUID taskId, java.time.Instant retryAt) {
    var task = findOrThrow(taskId);
    var waiting =
        new TaskModel(
            task.id(),
            task.projectId(),
            task.parentTaskId(),
            task.title(),
            task.description(),
            task.assignedTo(),
            TaskStatus.RATE_LIMITED,
            task.result(),
            task.createdAt(),
            timeProvider.now(),
            retryAt);
    return taskStore.save(waiting);
  }

  private TaskModel findOrThrow(UUID taskId) {
    return taskStore.findById(taskId).orElseThrow(() -> new NotFoundException("Task", taskId));
  }
}
