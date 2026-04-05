package org.blacksoil.devcrew.task.domain;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;

/** Доменная модель задачи. Immutable record. */
public record TaskModel(
    UUID id,
    UUID projectId,
    UUID parentTaskId,
    String title,
    String description,
    AgentRole assignedTo,
    TaskStatus status,
    String result,
    Instant createdAt,
    Instant updatedAt,
    /** Момент, после которого задачу можно повторить. Заполняется при статусе RATE_LIMITED. */
    Instant retryAt) {

  public TaskModel withStatus(TaskStatus status) {
    return new TaskModel(
        id,
        projectId,
        parentTaskId,
        title,
        description,
        assignedTo,
        status,
        result,
        createdAt,
        updatedAt,
        retryAt);
  }

  public TaskModel withResult(String result) {
    return new TaskModel(
        id,
        projectId,
        parentTaskId,
        title,
        description,
        assignedTo,
        status,
        result,
        createdAt,
        updatedAt,
        retryAt);
  }

  public TaskModel withRetryAt(Instant retryAt) {
    return new TaskModel(
        id,
        projectId,
        parentTaskId,
        title,
        description,
        assignedTo,
        status,
        result,
        createdAt,
        updatedAt,
        retryAt);
  }

  public TaskModel withUpdatedAt(Instant updatedAt) {
    return new TaskModel(
        id,
        projectId,
        parentTaskId,
        title,
        description,
        assignedTo,
        status,
        result,
        createdAt,
        updatedAt,
        retryAt);
  }
}
