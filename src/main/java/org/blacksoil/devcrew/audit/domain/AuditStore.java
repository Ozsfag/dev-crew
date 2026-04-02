package org.blacksoil.devcrew.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditStore {

    AuditEventModel save(AuditEventModel event);

    List<AuditEventModel> findByTimestampBetween(Instant from, Instant to);

    List<AuditEventModel> findByProjectIdAndTimestampBetween(UUID projectId, Instant from, Instant to);
}
