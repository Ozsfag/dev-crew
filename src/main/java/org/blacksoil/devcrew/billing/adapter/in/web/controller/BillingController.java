package org.blacksoil.devcrew.billing.adapter.in.web.controller;

import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.billing.adapter.in.web.dto.UsageSummaryResponse;
import org.blacksoil.devcrew.billing.adapter.in.web.mapper.BillingWebMapper;
import org.blacksoil.devcrew.billing.app.service.query.UsageQueryService;
import org.blacksoil.devcrew.bootstrap.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

  private final UsageQueryService usageQueryService;
  private final BillingWebMapper mapper;

  /**
   * Возвращает агрегированный отчёт об использовании за месяц.
   *
   * @param month год и месяц в формате YYYY-MM (например, "2026-01")
   */
  @GetMapping("/usage")
  public ResponseEntity<UsageSummaryResponse> getUsage(
      @RequestParam String month, @AuthenticationPrincipal AuthenticatedUser currentUser) {
    var yearMonth = YearMonth.parse(month);
    var summary = usageQueryService.getMonthlySummary(currentUser.orgId(), yearMonth);
    return ResponseEntity.ok(mapper.toResponse(summary));
  }
}
