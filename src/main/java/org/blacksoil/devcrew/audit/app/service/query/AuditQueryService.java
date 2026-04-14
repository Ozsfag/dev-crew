package org.blacksoil.devcrew.audit.app.service.query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.blacksoil.devcrew.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

  private final AuditStore auditStore;

  @Transactional(readOnly = true)
  public List<AuditEventModel> findByTimestampBetween(Instant from, Instant to) {
    return auditStore.findByTimestampBetween(from, to);
  }

  @Transactional(readOnly = true)
  public PageResult<AuditEventModel> findByTimestampBetween(
      Instant from, Instant to, int page, int size) {
    return auditStore.findByTimestampBetween(from, to, page, size);
  }

  @Transactional(readOnly = true)
  public List<AuditEventModel> findByProjectId(UUID projectId, Instant from, Instant to) {
    return auditStore.findByProjectIdAndTimestampBetween(projectId, from, to);
  }

  @Transactional(readOnly = true)
  public PageResult<AuditEventModel> findByProjectId(
      UUID projectId, Instant from, Instant to, int page, int size) {
    return auditStore.findByProjectIdAndTimestampBetween(projectId, from, to, page, size);
  }
}
