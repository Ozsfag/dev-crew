package org.blacksoil.devcrew.audit.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.audit.adapter.out.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

  List<AuditEventEntity> findByTimestampBetweenOrderByTimestampAsc(Instant from, Instant to);

  List<AuditEventEntity> findByProjectIdAndTimestampBetweenOrderByTimestampAsc(
      UUID projectId, Instant from, Instant to);
}
