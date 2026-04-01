package org.blacksoil.devcrew.audit.app.service.command;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditCommandService {

    private final AuditStore auditStore;

    @Transactional
    public void record(AuditEventModel event) {
        auditStore.save(event);
    }
}
