package org.blacksoil.devcrew.billing.adapter.in.hook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.blacksoil.devcrew.billing.app.service.query.UsageQueryService;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanLimitPreRunCheckTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private OrganizationQueryService organizationQueryService;
  @Mock private UsageQueryService usageQueryService;
  @Mock private TimeProvider timeProvider;

  private PlanLimitPreRunCheck check;
  private BillingProperties properties;

  @BeforeEach
  void setUp() {
    properties = new BillingProperties(); // freePlanTaskLimit = 50
    check =
        new PlanLimitPreRunCheck(
            organizationQueryService, usageQueryService, properties, timeProvider);
  }

  @Test
  void check_passes_when_projectId_is_null() {
    assertThatCode(() -> check.check(null)).doesNotThrowAnyException();
  }

  @Test
  void check_passes_for_pro_plan_regardless_of_usage() {
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    setupProject(projectId, orgId);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.PRO));

    assertThatCode(() -> check.check(projectId)).doesNotThrowAnyException();
  }

  @Test
  void check_passes_for_enterprise_plan() {
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    setupProject(projectId, orgId);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.ENTERPRISE));

    assertThatCode(() -> check.check(projectId)).doesNotThrowAnyException();
  }

  @Test
  void check_passes_for_free_plan_under_limit() {
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    var now = Instant.parse("2026-01-15T10:00:00Z");
    setupProject(projectId, orgId);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.FREE));
    when(timeProvider.now()).thenReturn(now);
    when(usageQueryService.countTasksThisMonth(orgId, YearMonth.of(2026, 1))).thenReturn(49);

    assertThatCode(() -> check.check(projectId)).doesNotThrowAnyException();
  }

  @Test
  void check_throws_when_free_plan_limit_reached() {
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    var now = Instant.parse("2026-01-15T10:00:00Z");
    setupProject(projectId, orgId);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.FREE));
    when(timeProvider.now()).thenReturn(now);
    when(usageQueryService.countTasksThisMonth(orgId, YearMonth.of(2026, 1))).thenReturn(50);

    assertThatThrownBy(() -> check.check(projectId)).isInstanceOf(DomainException.class);
  }

  @Test
  void check_throws_when_free_plan_limit_exceeded() {
    var projectId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    var now = Instant.parse("2026-01-15T10:00:00Z");
    setupProject(projectId, orgId);
    when(organizationQueryService.getById(orgId)).thenReturn(org(orgId, OrgPlan.FREE));
    when(timeProvider.now()).thenReturn(now);
    when(usageQueryService.countTasksThisMonth(orgId, YearMonth.of(2026, 1))).thenReturn(99);

    assertThatThrownBy(() -> check.check(projectId)).isInstanceOf(DomainException.class);
  }

  private void setupProject(UUID projectId, UUID orgId) {
    when(organizationQueryService.getProjectById(projectId))
        .thenReturn(new ProjectModel(projectId, orgId, "project", "/repo", NOW, NOW));
  }

  private OrganizationModel org(UUID id, OrgPlan plan) {
    return new OrganizationModel(id, "Org", plan, null, NOW, NOW);
  }
}
