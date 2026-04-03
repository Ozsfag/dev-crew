package org.blacksoil.devcrew.billing.adapter.out.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.blacksoil.devcrew.common.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UsageRecordJpaStoreTest extends IntegrationTestBase {

  @Autowired private UsageRecordJpaStore store;

  @Test
  void save_and_findByOrgIdAndMonth_roundtrip() {
    var orgId = UUID.randomUUID();
    var month = YearMonth.of(2026, 1);
    var record = usageRecord(orgId, Instant.parse("2026-01-15T10:00:00Z"));

    store.save(record);
    var found = store.findByOrgIdAndMonth(orgId, month);

    assertThat(found).hasSize(1);
    assertThat(found.get(0).orgId()).isEqualTo(orgId);
    assertThat(found.get(0).promptTokens()).isEqualTo(record.promptTokens());
    assertThat(found.get(0).costUsd()).isEqualByComparingTo(record.costUsd());
  }

  @Test
  void findByOrgIdAndMonth_excludes_different_org() {
    var orgId = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    var month = YearMonth.of(2026, 1);

    store.save(usageRecord(orgId, Instant.parse("2026-01-10T00:00:00Z")));
    store.save(usageRecord(otherId, Instant.parse("2026-01-10T00:00:00Z")));

    var found = store.findByOrgIdAndMonth(orgId, month);

    assertThat(found).hasSize(1);
    assertThat(found.get(0).orgId()).isEqualTo(orgId);
  }

  @Test
  void findByOrgIdAndMonth_excludes_different_month() {
    var orgId = UUID.randomUUID();

    store.save(usageRecord(orgId, Instant.parse("2026-01-31T23:59:59Z")));
    store.save(usageRecord(orgId, Instant.parse("2026-02-01T00:00:00Z")));

    var january = store.findByOrgIdAndMonth(orgId, YearMonth.of(2026, 1));
    var february = store.findByOrgIdAndMonth(orgId, YearMonth.of(2026, 2));

    assertThat(january).hasSize(1);
    assertThat(february).hasSize(1);
  }

  @Test
  void findByOrgIdAndMonth_returns_empty_for_no_records() {
    var found = store.findByOrgIdAndMonth(UUID.randomUUID(), YearMonth.of(2026, 1));

    assertThat(found).isEmpty();
  }

  private UsageRecordModel usageRecord(UUID orgId, Instant recordedAt) {
    return new UsageRecordModel(
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        orgId,
        AgentRole.BACKEND_DEV,
        100,
        200,
        new BigDecimal("0.00000300"),
        recordedAt);
  }
}
