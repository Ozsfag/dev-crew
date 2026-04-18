package org.blacksoil.devcrew.audit.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.audit.adapter.out.persistence.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

  List<AuditEventEntity> findByTimestampBetweenOrderByTimestampAsc(Instant from, Instant to);

  Page<AuditEventEntity> findByTimestampBetween(Instant from, Instant to, Pageable pageable);

  List<AuditEventEntity> findByProjectIdAndTimestampBetweenOrderByTimestampAsc(
      UUID projectId, Instant from, Instant to);

  Page<AuditEventEntity> findByProjectIdAndTimestampBetween(
      UUID projectId, Instant from, Instant to, Pageable pageable);

  Page<AuditEventEntity> findByActorId(UUID actorId, Pageable pageable);
}
