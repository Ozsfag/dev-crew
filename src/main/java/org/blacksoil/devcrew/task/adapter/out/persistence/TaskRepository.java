package org.blacksoil.devcrew.task.adapter.out.persistence;

import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByStatus(TaskStatus status);

    List<TaskEntity> findByParentTaskId(UUID parentTaskId);
}
