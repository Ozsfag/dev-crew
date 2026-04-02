package org.blacksoil.devcrew.audit.adapter.out.persistence;

import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("tc")
@Transactional
class AuditJpaStoreTest {

    @Autowired
    private AuditJpaStore auditJpaStore;

    @Test
    void save_and_findByTimestampBetween_returns_event() {
        var now = Instant.now();
        auditJpaStore.save(auditEvent(now, UUID.randomUUID()));

        var result = auditJpaStore.findByTimestampBetween(
            now.minusSeconds(1), now.plusSeconds(1)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).action()).isEqualTo("TASK_COMPLETED");
    }

    @Test
    void findByTimestampBetween_excludes_events_outside_range() {
        var past = Instant.now().minusSeconds(100);
        auditJpaStore.save(auditEvent(past, UUID.randomUUID()));

        var result = auditJpaStore.findByTimestampBetween(
            Instant.now().minusSeconds(10), Instant.now()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findByTimestampBetween_returns_multiple_events_ordered_by_timestamp() {
        var entityId = UUID.randomUUID();
        var t1 = Instant.now().minusSeconds(5);
        var t2 = Instant.now().minusSeconds(2);
        auditJpaStore.save(auditEvent(t2, entityId));
        auditJpaStore.save(auditEvent(t1, entityId));

        var result = auditJpaStore.findByTimestampBetween(
            t1.minusSeconds(1), Instant.now()
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).timestamp()).isBeforeOrEqualTo(result.get(1).timestamp());
    }

    private AuditEventModel auditEvent(Instant timestamp, UUID entityId) {
        return new AuditEventModel(
            UUID.randomUUID(),
            null,
            "system",
            "TASK_COMPLETED",
            entityId,
            "result details",
            timestamp
        );
    }
}
