package org.blacksoil.devcrew.task.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.common.PageResult;

/** Port-интерфейс хранилища задач. */
public interface TaskStore {

  TaskModel save(TaskModel task);

  Optional<TaskModel> findById(UUID id);

  List<TaskModel> findByStatus(TaskStatus status);

  List<TaskModel> findByParentTaskId(UUID parentTaskId);

  List<TaskModel> findByProjectId(UUID projectId);

  List<TaskModel> findByOrgId(UUID orgId);

  PageResult<TaskModel> findByOrgId(UUID orgId, int page, int size);

  /** Возвращает задачи со статусом RATE_LIMITED, чей retryAt уже наступил. */
  List<TaskModel> findRateLimitedReadyToRetry(java.time.Instant now);
}
