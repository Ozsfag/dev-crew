package org.blacksoil.devcrew.billing.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.blacksoil.devcrew.billing.adapter.in.web.mapper.BillingWebMapper;
import org.blacksoil.devcrew.billing.app.service.query.UsageQueryService;
import org.blacksoil.devcrew.billing.domain.UsageSummaryModel;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

  @Mock private UsageQueryService usageQueryService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller =
        new BillingController(usageQueryService, Mappers.getMapper(BillingWebMapper.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void GET_usage_returns_200_with_summary() throws Exception {
    var orgId = UUID.randomUUID();
    when(usageQueryService.getMonthlySummary(any(), any()))
        .thenReturn(
            summary(
                orgId, YearMonth.of(2026, 1), 10, 5000L, new BigDecimal("0.09000000"), -1, false));

    mockMvc
        .perform(
            get("/api/billing/usage").param("orgId", orgId.toString()).param("month", "2026-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orgId").value(orgId.toString()))
        .andExpect(jsonPath("$.month").value("2026-01"))
        .andExpect(jsonPath("$.totalTasks").value(10))
        .andExpect(jsonPath("$.totalTokens").value(5000))
        .andExpect(jsonPath("$.planLimit").value(-1))
        .andExpect(jsonPath("$.limitReached").value(false));
  }

  @Test
  void GET_usage_shows_limit_reached_for_free_plan() throws Exception {
    var orgId = UUID.randomUUID();
    when(usageQueryService.getMonthlySummary(any(), any()))
        .thenReturn(summary(orgId, YearMonth.of(2026, 1), 50, 100000L, BigDecimal.ZERO, 50, true));

    mockMvc
        .perform(
            get("/api/billing/usage").param("orgId", orgId.toString()).param("month", "2026-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.planLimit").value(50))
        .andExpect(jsonPath("$.limitReached").value(true));
  }

  @Test
  void GET_usage_returns_400_for_invalid_month_format() throws Exception {
    mockMvc
        .perform(
            get("/api/billing/usage")
                .param("orgId", UUID.randomUUID().toString())
                .param("month", "not-a-month"))
        .andExpect(status().isBadRequest());
  }

  private UsageSummaryModel summary(
      UUID orgId,
      YearMonth month,
      int tasks,
      long tokens,
      BigDecimal cost,
      int limit,
      boolean limitReached) {
    return new UsageSummaryModel(orgId, month, tasks, tokens, cost, limit, limitReached);
  }
}
