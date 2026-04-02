package org.blacksoil.devcrew.audit.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    List<AuditEventEntity> findByTimestampBetweenOrderByTimestampAsc(Instant from, Instant to);

    List<AuditEventEntity> findByProjectIdAndTimestampBetweenOrderByTimestampAsc(UUID projectId, Instant from, Instant to);
}
