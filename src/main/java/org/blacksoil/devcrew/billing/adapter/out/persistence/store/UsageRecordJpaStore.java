package org.blacksoil.devcrew.billing.adapter.out.persistence.store;

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
  public boolean existsByTaskId(UUID taskId) {
    return repository.existsByTaskId(taskId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UsageRecordModel> findByOrgIdAndMonth(UUID orgId, YearMonth month) {
    // Полуоткрытый интервал [начало месяца, начало следующего). Инклюзивный верхний
    // LocalTime.MAX (.999999999) округлялся PostgreSQL (timestamptz, микросекунды)
    // вверх до 00:00:00 следующего месяца и затягивал первую запись соседнего месяца.
    var from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    var toExclusive = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return repository
        .findByOrgIdAndRecordedAtGreaterThanEqualAndRecordedAtLessThan(orgId, from, toExclusive)
        .stream()
        .map(mapper::toModel)
        .toList();
  }
}
