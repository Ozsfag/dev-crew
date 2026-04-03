package org.blacksoil.devcrew.task.adapter.out.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("tc")
@Transactional
class TaskJpaStoreTest {

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
    taskJpaStore.save(taskModel(null, AgentRole.PM, TaskStatus.PENDING));

    var pending = taskJpaStore.findByStatus(TaskStatus.PENDING);

    assertThat(pending).hasSize(2);
    assertThat(pending).allMatch(t -> t.status() == TaskStatus.PENDING);
  }

  @Test
  void findByParentTaskId_returns_subtasks() {
    var parent = taskJpaStore.save(taskModel(null, AgentRole.ORCHESTRATOR, TaskStatus.IN_PROGRESS));
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
            saved.parentTaskId(),
            saved.title(),
            saved.description(),
            saved.assignedTo(),
            TaskStatus.COMPLETED,
            "result text",
            saved.createdAt(),
            Instant.now());

    var result = taskJpaStore.save(updated);

    assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(result.result()).isEqualTo("result text");
  }

  private TaskModel taskModel(UUID parentId, AgentRole role, TaskStatus status) {
    return new TaskModel(
        UUID.randomUUID(),
        null,
        parentId,
        "Task for " + role,
        "Description",
        role,
        status,
        null,
        Instant.now(),
        Instant.now());
  }
}
