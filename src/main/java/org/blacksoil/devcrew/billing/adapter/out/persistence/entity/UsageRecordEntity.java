package org.blacksoil.devcrew.billing.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentRole;

@Entity
@Table(
    name = "usage_records",
    indexes = @Index(name = "idx_usage_records_org_month", columnList = "org_id, recorded_at"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordEntity {

  @Id private UUID id;

  @Column(name = "task_id", nullable = false)
  private UUID taskId;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "org_id", nullable = false)
  private UUID orgId;

  @Enumerated(EnumType.STRING)
  @Column(name = "agent_role", nullable = false)
  private AgentRole agentRole;

  @Column(name = "prompt_tokens", nullable = false)
  private int promptTokens;

  @Column(name = "completion_tokens", nullable = false)
  private int completionTokens;

  @Column(name = "cost_usd", nullable = false, precision = 12, scale = 8)
  private BigDecimal costUsd;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;
}
