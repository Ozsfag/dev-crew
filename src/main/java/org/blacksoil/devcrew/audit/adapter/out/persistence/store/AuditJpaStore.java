package org.blacksoil.devcrew.audit.adapter.out.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.adapter.out.persistence.mapper.AuditPersistenceMapper;
import org.blacksoil.devcrew.audit.adapter.out.persistence.repository.AuditEventRepository;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    return auditEventRepository.findByTimestampBetweenOrderByTimestampAsc(from, to).stream()
        .map(mapper::toModel)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AuditEventModel> findByProjectIdAndTimestampBetween(
      UUID projectId, Instant from, Instant to) {
    return auditEventRepository
        .findByProjectIdAndTimestampBetweenOrderByTimestampAsc(projectId, from, to)
        .stream()
        .map(mapper::toModel)
        .toList();
  }
}
