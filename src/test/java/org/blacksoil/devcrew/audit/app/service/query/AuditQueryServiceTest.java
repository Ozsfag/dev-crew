package org.blacksoil.devcrew.audit.app.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

  @Mock private AuditStore auditStore;

  @InjectMocks private AuditQueryService service;

  private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-01-01T23:59:59Z");

  @Test
  void findByTimestampBetween_returns_events_from_store() {
    var expected = List.of(auditEvent());
    when(auditStore.findByTimestampBetween(FROM, TO)).thenReturn(expected);

    var result = service.findByTimestampBetween(FROM, TO);

    assertThat(result).isEqualTo(expected);
    verify(auditStore).findByTimestampBetween(FROM, TO);
  }

  @Test
  void findByTimestampBetween_returns_empty_list_when_no_events() {
    when(auditStore.findByTimestampBetween(FROM, TO)).thenReturn(List.of());

    var result = service.findByTimestampBetween(FROM, TO);

    assertThat(result).isEmpty();
  }

  @Test
  void findByProjectId_returns_events_for_project() {
    var projectId = UUID.randomUUID();
    var expected = List.of(auditEvent());
    when(auditStore.findByProjectIdAndTimestampBetween(projectId, FROM, TO)).thenReturn(expected);

    var result = service.findByProjectId(projectId, FROM, TO);

    assertThat(result).isEqualTo(expected);
    verify(auditStore).findByProjectIdAndTimestampBetween(projectId, FROM, TO);
  }

  @Test
  void findByProjectId_returns_empty_list_when_no_events_for_project() {
    var projectId = UUID.randomUUID();
    when(auditStore.findByProjectIdAndTimestampBetween(projectId, FROM, TO)).thenReturn(List.of());

    var result = service.findByProjectId(projectId, FROM, TO);

    assertThat(result).isEmpty();
  }

  private AuditEventModel auditEvent() {
    return new AuditEventModel(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "user@example.com",
        "TASK_CREATED",
        UUID.randomUUID(),
        "details",
        Instant.parse("2026-01-01T10:00:00Z"));
  }
}
