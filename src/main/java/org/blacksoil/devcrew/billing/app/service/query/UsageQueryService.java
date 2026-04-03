package org.blacksoil.devcrew.billing.app.service.query;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.blacksoil.devcrew.billing.domain.UsageRecordStore;
import org.blacksoil.devcrew.billing.domain.UsageSummaryModel;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsageQueryService {

  private final UsageRecordStore usageRecordStore;
  private final OrganizationQueryService organizationQueryService;
  private final BillingProperties billingProperties;

  /** Возвращает агрегированный отчёт по использованию за указанный месяц. */
  public UsageSummaryModel getMonthlySummary(UUID orgId, YearMonth month) {
    var org = organizationQueryService.getById(orgId);
    var records = usageRecordStore.findByOrgIdAndMonth(orgId, month);
    long totalTokens = sumTokens(records);
    var totalCost = sumCost(records);
    int planLimit = org.plan() == OrgPlan.FREE ? billingProperties.getFreePlanTaskLimit() : -1;
    return new UsageSummaryModel(
        orgId,
        month,
        records.size(),
        totalTokens,
        totalCost,
        planLimit,
        planLimit > 0 && records.size() >= planLimit);
  }

  /** Возвращает количество выполненных задач за месяц для проверки лимита плана. */
  public int countTasksThisMonth(UUID orgId, YearMonth month) {
    return usageRecordStore.findByOrgIdAndMonth(orgId, month).size();
  }

  private long sumTokens(List<UsageRecordModel> records) {
    return records.stream().mapToLong(r -> r.promptTokens() + r.completionTokens()).sum();
  }

  private BigDecimal sumCost(List<UsageRecordModel> records) {
    return records.stream().map(UsageRecordModel::costUsd).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
