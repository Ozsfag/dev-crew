package org.blacksoil.devcrew.audit.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuditJpaStore implements AuditStore {

    private final AuditEventRepository auditEventRepository;
    private final AuditPersistenceMapper mapper;

    @Override
    @Transactional
    public AuditEventModel save(AuditEventModel event) {
        return mapper.toModel(auditEventRepository.save(mapper.toEntity(event)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventModel> findByTimestampBetween(Instant from, Instant to) {
        return auditEventRepository
            .findByTimestampBetweenOrderByTimestampAsc(from, to)
            .stream()
            .map(mapper::toModel)
            .toList();
    }
}
