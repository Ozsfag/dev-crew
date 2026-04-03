package org.blacksoil.devcrew.billing.adapter.in.hook;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.PreRunCheck;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.blacksoil.devcrew.billing.app.service.query.UsageQueryService;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.springframework.stereotype.Component;

/**
 * Проверяет лимит задач по плану организации перед запуском агента. Организации на плане FREE
 * ограничены 50 задачами в месяц. PRO и ENTERPRISE — без ограничений.
 */
@Component
@RequiredArgsConstructor
public class PlanLimitPreRunCheck implements PreRunCheck {

  private final OrganizationQueryService organizationQueryService;
  private final UsageQueryService usageQueryService;
  private final BillingProperties billingProperties;
  private final TimeProvider timeProvider;

  @Override
  public void check(UUID projectId) {
    if (projectId == null) {
      return;
    }
    var project = organizationQueryService.getProjectById(projectId);
    var org = organizationQueryService.getById(project.orgId());
    if (org.plan() != OrgPlan.FREE) {
      return;
    }
    var month = YearMonth.from(timeProvider.now().atZone(ZoneOffset.UTC));
    int used = usageQueryService.countTasksThisMonth(org.id(), month);
    if (used >= billingProperties.getFreePlanTaskLimit()) {
      throw new DomainException(
          "Достигнут лимит плана FREE: "
              + billingProperties.getFreePlanTaskLimit()
              + " задач в месяц. Перейдите на план PRO.");
    }
  }
}
