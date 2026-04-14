package org.blacksoil.devcrew.task.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.task.adapter.out.persistence.entity.TaskEntity;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

  List<TaskEntity> findByStatus(TaskStatus status);

  List<TaskEntity> findByParentTaskId(UUID parentTaskId);

  List<TaskEntity> findByProjectId(UUID projectId);

  @Query(
      value =
          "SELECT t.* FROM tasks t JOIN projects p ON t.project_id = p.id WHERE p.org_id = :orgId",
      nativeQuery = true)
  List<TaskEntity> findByOrgId(@Param("orgId") UUID orgId);

  @Query(
      value =
          "SELECT t.* FROM tasks t JOIN projects p ON t.project_id = p.id WHERE p.org_id = :orgId",
      countQuery =
          "SELECT count(*) FROM tasks t JOIN projects p ON t.project_id = p.id"
              + " WHERE p.org_id = :orgId",
      nativeQuery = true)
  Page<TaskEntity> findByOrgId(@Param("orgId") UUID orgId, Pageable pageable);

  List<TaskEntity> findByStatusAndRetryAtBefore(TaskStatus status, Instant retryAt);
}
