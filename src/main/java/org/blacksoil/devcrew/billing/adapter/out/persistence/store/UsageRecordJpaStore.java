package org.blacksoil.devcrew.billing.adapter.out.persistence.store;

import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.billing.adapter.out.persistence.mapper.UsageRecordPersistenceMapper;
import org.blacksoil.devcrew.billing.adapter.out.persistence.repository.UsageRecordRepository;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.blacksoil.devcrew.billing.domain.UsageRecordStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UsageRecordJpaStore implements UsageRecordStore {

  private final UsageRecordRepository repository;
  private final UsageRecordPersistenceMapper mapper;

  @Override
  @Transactional
  public UsageRecordModel save(UsageRecordModel record) {
    return mapper.toModel(repository.save(mapper.toEntity(record)));
  }

  @Override
  @Transactional(readOnly = true)
  public List<UsageRecordModel> findByOrgIdAndMonth(UUID orgId, YearMonth month) {
    var from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    var to = month.atEndOfMonth().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    return repository.findByOrgIdAndRecordedAtBetween(orgId, from, to).stream()
        .map(mapper::toModel)
        .toList();
  }
}
