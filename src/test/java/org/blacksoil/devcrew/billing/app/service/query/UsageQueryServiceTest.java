package org.blacksoil.devcrew.billing.app.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.blacksoil.devcrew.billing.domain.UsageRecordStore;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.OrganizationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageQueryServiceTest {

  @Mock private UsageRecordStore usageRecordStore;
  @Mock private OrganizationQueryService organizationQueryService;

  private UsageQueryService service;
  private BillingProperties properties;

  @BeforeEach
  void setUp() {
    properties = new BillingProperties();
    service = new UsageQueryService(usageRecordStore, organizationQueryService, properties);
  }

  @Test
  void getMonthlySummary_aggregates_tokens_and_cost() {
    var orgId = UUID.randomUUID();
    var month = YearMonth.of(2026, 1);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.PRO));
    when(usageRecordStore.findByOrgIdAndMonth(orgId, month))
        .thenReturn(
            List.of(
                usageRecord(orgId, 100, 200, new BigDecimal("0.00360000")),
                usageRecord(orgId, 50, 100, new BigDecimal("0.00180000"))));

    var summary = service.getMonthlySummary(orgId, month);

    assertThat(summary.totalTasks()).isEqualTo(2);
    assertThat(summary.totalTokens()).isEqualTo(450L);
    assertThat(summary.totalCostUsd()).isEqualByComparingTo(new BigDecimal("0.00540000"));
  }

  @Test
  void getMonthlySummary_sets_plan_limit_minus_one_for_pro() {
    var orgId = UUID.randomUUID();
    var month = YearMonth.of(2026, 1);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.PRO));
    when(usageRecordStore.findByOrgIdAndMonth(orgId, month)).thenReturn(List.of());

    var summary = service.getMonthlySummary(orgId, month);

    assertThat(summary.planLimit()).isEqualTo(-1);
    assertThat(summary.limitReached()).isFalse();
  }

  @Test
  void getMonthlySummary_sets_plan_limit_for_free_plan() {
    var orgId = UUID.randomUUID();
    var month = YearMonth.of(2026, 1);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.FREE));
    when(usageRecordStore.findByOrgIdAndMonth(orgId, month)).thenReturn(List.of());

    var summary = service.getMonthlySummary(orgId, month);

    assertThat(summary.planLimit()).isEqualTo(properties.getFreePlanTaskLimit());
  }

  @Test
  void getMonthlySummary_detects_limit_reached_for_free_plan() {
    var orgId = UUID.randomUUID();
    var month = YearMonth.of(2026, 1);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.FREE));
    // 50 записей = лимит достигнут
    var records =
        java.util.stream.IntStream.range(0, 50)
            .mapToObj(i -> usageRecord(orgId, 10, 10, BigDecimal.ZERO))
            .toList();
    when(usageRecordStore.findByOrgIdAndMonth(orgId, month)).thenReturn(records);

    var summary = service.getMonthlySummary(orgId, month);

    assertThat(summary.limitReached()).isTrue();
  }

  @Test
  void countTasksThisMonth_returns_record_count() {
    var orgId = UUID.randomUUID();
    var month = YearMonth.of(2026, 1);
    when(usageRecordStore.findByOrgIdAndMonth(orgId, month))
        .thenReturn(List.of(usageRecord(orgId, 1, 1, BigDecimal.ZERO)));

    assertThat(service.countTasksThisMonth(orgId, month)).isEqualTo(1);
  }

  private OrganizationModel org(UUID id, OrgPlan plan) {
    return new OrganizationModel(id, "Test Org", plan, Instant.now(), Instant.now());
  }

  private UsageRecordModel usageRecord(UUID orgId, int prompt, int completion, BigDecimal cost) {
    return new UsageRecordModel(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        orgId,
        AgentRole.BACKEND_DEV,
        prompt,
        completion,
        cost,
        Instant.now());
  }
}
