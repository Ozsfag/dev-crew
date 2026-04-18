package org.blacksoil.devcrew.audit.adapter.out.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.common.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditJpaStoreTest extends IntegrationTestBase {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Autowired private AuditJpaStore auditJpaStore;

  @Test
  void save_and_findByTimestampBetween_returns_event() {
    var now = NOW;
    auditJpaStore.save(auditEvent(now, UUID.randomUUID()));

    var result = auditJpaStore.findByTimestampBetween(now.minusSeconds(1), now.plusSeconds(1));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).action()).isEqualTo("TASK_COMPLETED");
  }

  @Test
  void findByTimestampBetween_excludes_events_outside_range() {
    var past = NOW.minusSeconds(100);
    auditJpaStore.save(auditEvent(past, UUID.randomUUID()));

    var result = auditJpaStore.findByTimestampBetween(NOW.minusSeconds(10), NOW);

    assertThat(result).isEmpty();
  }

  @Test
  void findByTimestampBetween_returns_multiple_events_ordered_by_timestamp() {
    var entityId = UUID.randomUUID();
    var t1 = NOW.minusSeconds(5);
    var t2 = NOW.minusSeconds(2);
    auditJpaStore.save(auditEvent(t2, entityId));
    auditJpaStore.save(auditEvent(t1, entityId));

    var result = auditJpaStore.findByTimestampBetween(t1.minusSeconds(1), NOW);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).timestamp()).isBeforeOrEqualTo(result.get(1).timestamp());
  }

  @Test
  void findByTimestampBetween_clamps_size_to_100_when_requested_size_exceeds_limit() {
    var result =
        auditJpaStore.findByTimestampBetween(NOW.minusSeconds(1), NOW.plusSeconds(1), 0, 99999);

    assertThat(result.size()).isEqualTo(100);
  }

  private AuditEventModel auditEvent(Instant timestamp, UUID entityId) {
    return new AuditEventModel(
        UUID.randomUUID(),
        null,
        null,
        "system",
        "TASK_COMPLETED",
        entityId,
        "result details",
        timestamp);
  }
}
