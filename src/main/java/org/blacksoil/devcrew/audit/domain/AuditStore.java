package org.blacksoil.devcrew.audit.domain;

import java.time.Instant;
import java.util.List;

public interface AuditStore {

    AuditEventModel save(AuditEventModel event);

    List<AuditEventModel> findByTimestampBetween(Instant from, Instant to);
}
