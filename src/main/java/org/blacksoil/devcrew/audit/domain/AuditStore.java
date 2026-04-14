package org.blacksoil.devcrew.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.common.PageResult;

public interface AuditStore {

  AuditEventModel save(AuditEventModel event);

  List<AuditEventModel> findByTimestampBetween(Instant from, Instant to);

  PageResult<AuditEventModel> findByTimestampBetween(Instant from, Instant to, int page, int size);

  List<AuditEventModel> findByProjectIdAndTimestampBetween(
      UUID projectId, Instant from, Instant to);

  PageResult<AuditEventModel> findByProjectIdAndTimestampBetween(
      UUID projectId, Instant from, Instant to, int page, int size);
}
