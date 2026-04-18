package org.blacksoil.devcrew.task.adapter.out.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.IntegrationTestBase;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TaskJpaStoreTest extends IntegrationTestBase {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Autowired private TaskJpaStore taskJpaStore;

  @Test
  void save_and_findById_roundtrip() {
    var task = taskModel(null, AgentRole.BACKEND_DEV, TaskStatus.PENDING);

    var saved = taskJpaStore.save(task);
    var found = taskJpaStore.findById(saved.id());

    assertThat(found).isPresent();
    assertThat(found.get().title()).isEqualTo(task.title());
    assertThat(found.get().assignedTo()).isEqualTo(AgentRole.BACKEND_DEV);
    assertThat(found.get().status()).isEqualTo(TaskStatus.PENDING);
  }

  @Test
  void findByStatus_returns_matching_tasks() {
    taskJpaStore.save(taskModel(null, AgentRole.QA, TaskStatus.PENDING));
    taskJpaStore.save(taskModel(null, AgentRole.DEVOPS, TaskStatus.IN_PROGRESS));
    taskJpaStore.save(taskModel(null, AgentRole.CODE_REVIEWER, TaskStatus.PENDING));

    var pending = taskJpaStore.findByStatus(TaskStatus.PENDING);

    assertThat(pending).hasSize(2);
    assertThat(pending).allMatch(t -> t.status() == TaskStatus.PENDING);
  }

  @Test
  void findByParentTaskId_returns_subtasks() {
    var parent = taskJpaStore.save(taskModel(null, AgentRole.DOC_WRITER, TaskStatus.IN_PROGRESS));
    taskJpaStore.save(taskModel(parent.id(), AgentRole.BACKEND_DEV, TaskStatus.PENDING));
    taskJpaStore.save(taskModel(parent.id(), AgentRole.QA, TaskStatus.PENDING));
    taskJpaStore.save(taskModel(null, AgentRole.DEVOPS, TaskStatus.PENDING));

    var subtasks = taskJpaStore.findByParentTaskId(parent.id());

    assertThat(subtasks).hasSize(2);
    assertThat(subtasks).allMatch(t -> parent.id().equals(t.parentTaskId()));
  }

  @Test
  void save_updates_existing_task() {
    var saved = taskJpaStore.save(taskModel(null, AgentRole.BACKEND_DEV, TaskStatus.PENDING));
    var updated =
        new TaskModel(
            saved.id(),
            saved.projectId(),
            saved.orgId(),
            saved.parentTaskId(),
            saved.title(),
            saved.description(),
            saved.assignedTo(),
            TaskStatus.COMPLETED,
            "result text",
            null,
            saved.createdAt(),
            NOW,
            null);

    var result = taskJpaStore.save(updated);

    assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(result.result()).isEqualTo("result text");
  }

  @Test
  void findByOrgId_clamps_size_to_100_when_requested_size_exceeds_limit() {
    var result = taskJpaStore.findByOrgId(UUID.randomUUID(), 0, 99999);

    assertThat(result.size()).isEqualTo(100);
  }

  private TaskModel taskModel(UUID parentId, AgentRole role, TaskStatus status) {
    return new TaskModel(
        UUID.randomUUID(),
        null,
        null,
        parentId,
        "Task for " + role,
        "Description",
        role,
        status,
        null,
        null,
        NOW,
        NOW,
        null);
  }
}
