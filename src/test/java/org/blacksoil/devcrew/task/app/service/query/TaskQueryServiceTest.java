package org.blacksoil.devcrew.task.app.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.PageResult;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.blacksoil.devcrew.task.domain.TaskStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskQueryServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private TaskStore taskStore;

  @InjectMocks private TaskQueryService taskQueryService;

  @Test
  void getById_returns_task_when_found() {
    var id = UUID.randomUUID();
    var task = taskModel(id, null, TaskStatus.PENDING);
    when(taskStore.findById(id)).thenReturn(Optional.of(task));

    var result = taskQueryService.getById(id);

    assertThat(result).isEqualTo(task);
  }

  @Test
  void getById_throws_not_found_when_missing() {
    var id = UUID.randomUUID();
    when(taskStore.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskQueryService.getById(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void getByStatus_returns_matching_tasks() {
    var tasks =
        List.of(
            taskModel(UUID.randomUUID(), null, TaskStatus.PENDING),
            taskModel(UUID.randomUUID(), null, TaskStatus.PENDING));
    when(taskStore.findByStatus(TaskStatus.PENDING)).thenReturn(tasks);

    var result = taskQueryService.getByStatus(TaskStatus.PENDING);

    assertThat(result).hasSize(2);
  }

  @Test
  void getByOrgId_returns_tasks_for_org() {
    var orgId = UUID.randomUUID();
    var tasks = List.of(taskModel(UUID.randomUUID(), null, TaskStatus.PENDING));
    when(taskStore.findByOrgId(orgId)).thenReturn(tasks);

    var result = taskQueryService.getByOrgId(orgId);

    assertThat(result).hasSize(1);
  }

  @Test
  void getByOrgId_pageable_returns_page() {
    var orgId = UUID.randomUUID();
    var tasks = List.of(taskModel(UUID.randomUUID(), null, TaskStatus.PENDING));
    when(taskStore.findByOrgId(orgId, 0, 10)).thenReturn(new PageResult<>(tasks, 0, 10, 1));

    var result = taskQueryService.getByOrgId(orgId, 0, 10);

    assertThat(result.content()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(1);
  }

  @Test
  void getSubtasks_returns_children_of_parent() {
    var parentId = UUID.randomUUID();
    var subtasks = List.of(taskModel(UUID.randomUUID(), parentId, TaskStatus.PENDING));
    when(taskStore.findByParentTaskId(parentId)).thenReturn(subtasks);

    var result = taskQueryService.getSubtasks(parentId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).parentTaskId()).isEqualTo(parentId);
  }

  @Test
  void getByProjectId_returns_tasks_for_project() {
    var projectId = UUID.randomUUID();
    var tasks = List.of(taskModel(UUID.randomUUID(), null, TaskStatus.PENDING));
    when(taskStore.findByProjectId(projectId)).thenReturn(tasks);

    var result = taskQueryService.getByProjectId(projectId);

    assertThat(result).hasSize(1);
  }

  @Test
  void getRateLimitedReadyToRetry_returns_tasks_whose_retryAt_has_passed() {
    var tasks =
        List.of(
            taskModel(UUID.randomUUID(), null, TaskStatus.RATE_LIMITED),
            taskModel(UUID.randomUUID(), null, TaskStatus.RATE_LIMITED));
    when(taskStore.findRateLimitedReadyToRetry(NOW)).thenReturn(tasks);

    var result = taskQueryService.getRateLimitedReadyToRetry(NOW);

    assertThat(result).hasSize(2);
  }

  private TaskModel taskModel(UUID id, UUID parentId, TaskStatus status) {
    return new TaskModel(
        id,
        null,
        null,
        parentId,
        "title",
        "description",
        AgentRole.BACKEND_DEV,
        status,
        null,
        NOW,
        NOW,
        null);
  }
}
