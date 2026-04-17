package org.blacksoil.devcrew.task.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.task.adapter.out.persistence.entity.TaskEntity;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

  List<TaskEntity> findByStatus(TaskStatus status);

  List<TaskEntity> findByParentTaskId(UUID parentTaskId);

  List<TaskEntity> findByProjectId(UUID projectId);

  List<TaskEntity> findByOrgId(UUID orgId);

  Page<TaskEntity> findByOrgId(UUID orgId, Pageable pageable);

  List<TaskEntity> findByStatusAndRetryAtBefore(TaskStatus status, Instant retryAt);
}
