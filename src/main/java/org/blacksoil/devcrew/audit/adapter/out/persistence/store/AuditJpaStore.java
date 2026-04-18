package org.blacksoil.devcrew.audit.adapter.out.persistence.store;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.adapter.out.persistence.mapper.AuditPersistenceMapper;
import org.blacksoil.devcrew.audit.adapter.out.persistence.repository.AuditEventRepository;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.blacksoil.devcrew.common.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AuditJpaStore implements AuditStore {

  private static final int MAX_PAGE_SIZE = 100;

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
  public PageResult<AuditEventModel> findByTimestampBetween(
      Instant from, Instant to, int page, int size) {
    var pageable =
        PageRequest.of(
            page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "timestamp"));
    var result = auditEventRepository.findByTimestampBetween(from, to, pageable);
    return new PageResult<>(
        result.getContent().stream().map(mapper::toModel).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements());
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

  @Override
  @Transactional(readOnly = true)
  public PageResult<AuditEventModel> findByProjectIdAndTimestampBetween(
      UUID projectId, Instant from, Instant to, int page, int size) {
    var pageable =
        PageRequest.of(
            page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "timestamp"));
    var result =
        auditEventRepository.findByProjectIdAndTimestampBetween(projectId, from, to, pageable);
    return new PageResult<>(
        result.getContent().stream().map(mapper::toModel).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<AuditEventModel> findByActorId(UUID actorId, int page, int size) {
    var pageable =
        PageRequest.of(
            page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "timestamp"));
    var result = auditEventRepository.findByActorId(actorId, pageable);
    return new PageResult<>(
        result.getContent().stream().map(mapper::toModel).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements());
  }
}
