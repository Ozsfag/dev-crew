package org.blacksoil.devcrew.billing.app.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.blacksoil.devcrew.billing.app.policy.TokenEstimationPolicy;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.blacksoil.devcrew.billing.domain.UsageRecordStore;
import org.blacksoil.devcrew.common.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageRecordCommandServiceTest {

  @Mock private UsageRecordStore usageRecordStore;
  @Mock private TimeProvider timeProvider;

  private UsageRecordCommandService service;

  @BeforeEach
  void setUp() {
    service =
        new UsageRecordCommandService(
            usageRecordStore, new TokenEstimationPolicy(new BillingProperties()), timeProvider);
  }

  @Test
  void record_saves_usage_record_with_estimated_tokens() {
    var now = Instant.parse("2026-01-01T10:00:00Z");
    var taskId = UUID.randomUUID();
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    when(timeProvider.now()).thenReturn(now);
    when(usageRecordStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // prompt = 8 chars → 2 tokens; result = 40 chars → 10 tokens
    service.record(taskId, projectId, orgId, AgentRole.BACKEND_DEV, "12345678", "a".repeat(40));

    var captor = ArgumentCaptor.<UsageRecordModel>captor();
    verify(usageRecordStore).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.taskId()).isEqualTo(taskId);
    assertThat(saved.orgId()).isEqualTo(orgId);
    assertThat(saved.agentRole()).isEqualTo(AgentRole.BACKEND_DEV);
    assertThat(saved.promptTokens()).isEqualTo(2);
    assertThat(saved.completionTokens()).isEqualTo(10);
    assertThat(saved.recordedAt()).isEqualTo(now);
  }

  @Test
  void record_calculates_cost_based_on_tokens() {
    when(timeProvider.now()).thenReturn(Instant.now());
    when(usageRecordStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.record(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AgentRole.QA, "", "");

    var captor = ArgumentCaptor.<UsageRecordModel>captor();
    verify(usageRecordStore).save(captor.capture());
    assertThat(captor.getValue().costUsd()).isNotNull();
    assertThat(captor.getValue().costUsd().scale()).isEqualTo(8);
  }

  @Test
  void record_generates_unique_id() {
    when(timeProvider.now()).thenReturn(Instant.now());
    when(usageRecordStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.record(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        AgentRole.DEVOPS,
        "prompt",
        "result");

    var captor = ArgumentCaptor.<UsageRecordModel>captor();
    verify(usageRecordStore).save(captor.capture());
    assertThat(captor.getValue().id()).isNotNull();
  }
}
