package org.blacksoil.devcrew.audit.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventEntity {

  @Id private UUID id;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "actor_email", nullable = false)
  private String actorEmail;

  @Column(nullable = false)
  private String action;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(columnDefinition = "TEXT")
  private String details;

  @Column(nullable = false)
  private Instant timestamp;
}
