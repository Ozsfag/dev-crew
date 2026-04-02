package org.blacksoil.devcrew.audit.app.service.query;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditStore auditStore;

    @Transactional(readOnly = true)
    public List<AuditEventModel> findByTimestampBetween(Instant from, Instant to) {
        return auditStore.findByTimestampBetween(from, to);
    }

    @Transactional(readOnly = true)
    public List<AuditEventModel> findByProjectId(UUID projectId, Instant from, Instant to) {
        return auditStore.findByProjectIdAndTimestampBetween(projectId, from, to);
    }
}
