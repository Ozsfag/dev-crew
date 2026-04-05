package org.blacksoil.devcrew.billing.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.billing.adapter.out.persistence.entity.UsageRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRecordRepository extends JpaRepository<UsageRecordEntity, UUID> {

  boolean existsByTaskId(UUID taskId);

  List<UsageRecordEntity> findByOrgIdAndRecordedAtBetween(UUID orgId, Instant from, Instant to);
}
