package org.blacksoil.devcrew.task.app.service.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.common.PageResult;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskQueryService {

  private final TaskStore taskStore;

  public TaskModel getById(UUID id) {
    return taskStore.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
  }

  public List<TaskModel> getByStatus(TaskStatus status) {
    return taskStore.findByStatus(status);
  }

  public List<TaskModel> getSubtasks(UUID parentTaskId) {
    return taskStore.findByParentTaskId(parentTaskId);
  }

  public List<TaskModel> getByProjectId(UUID projectId) {
    return taskStore.findByProjectId(projectId);
  }

  public List<TaskModel> getByOrgId(UUID orgId) {
    return taskStore.findByOrgId(orgId);
  }

  public PageResult<TaskModel> getByOrgId(UUID orgId, int page, int size) {
    return taskStore.findByOrgId(orgId, page, size);
  }

  /** Возвращает задачи RATE_LIMITED, чей retryAt уже наступил. */
  public List<TaskModel> getRateLimitedReadyToRetry(Instant now) {
    return taskStore.findRateLimitedReadyToRetry(now);
  }
}
