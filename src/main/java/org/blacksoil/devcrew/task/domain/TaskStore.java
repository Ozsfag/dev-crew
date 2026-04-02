package org.blacksoil.devcrew.task.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port-интерфейс хранилища задач.
 */
public interface TaskStore {

    TaskModel save(TaskModel task);

    Optional<TaskModel> findById(UUID id);

    List<TaskModel> findByStatus(TaskStatus status);

    List<TaskModel> findByParentTaskId(UUID parentTaskId);

    List<TaskModel> findByProjectId(UUID projectId);
}
