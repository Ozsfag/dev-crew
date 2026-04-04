package org.blacksoil.devcrew.audit.app.service.command;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.audit.domain.AuditStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditCommandServiceTest {

  @Mock private AuditStore auditStore;

  @InjectMocks private AuditCommandService service;

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Test
  void record_saves_event_to_store() {
    var event = auditEvent();

    service.record(event);

    verify(auditStore).save(event);
  }

  private AuditEventModel auditEvent() {
    return new AuditEventModel(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "user@example.com",
        "TASK_CREATED",
        UUID.randomUUID(),
        "details",
        NOW);
  }
}
