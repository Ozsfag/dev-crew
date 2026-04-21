package org.blacksoil.devcrew.billing.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.blacksoil.devcrew.billing.adapter.in.web.mapper.BillingWebMapper;
import org.blacksoil.devcrew.billing.app.service.query.UsageQueryService;
import org.blacksoil.devcrew.billing.domain.UsageSummaryModel;
import org.blacksoil.devcrew.bootstrap.AuthenticatedUser;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final AuthenticatedUser CURRENT_USER =
      new AuthenticatedUser(USER_ID, ORG_ID, UserRole.ARCHITECT);

  @Mock private UsageQueryService usageQueryService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller =
        new BillingController(usageQueryService, Mappers.getMapper(BillingWebMapper.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(principalResolver(CURRENT_USER))
            .build();
  }

  @Test
  void GET_usage_uses_current_user_org_id() throws Exception {
    when(usageQueryService.getMonthlySummary(any(), any()))
        .thenReturn(summary(ORG_ID, YearMonth.of(2026, 1), 10, 5000L, BigDecimal.ZERO, -1, false));

    mockMvc.perform(get("/api/billing/usage").param("month", "2026-01")).andExpect(status().isOk());

    // orgId должен приходить из principal, не из request param
    verify(usageQueryService).getMonthlySummary(eq(ORG_ID), any());
  }

  @Test
  void GET_usage_returns_200_with_summary() throws Exception {
    when(usageQueryService.getMonthlySummary(any(), any()))
        .thenReturn(
            summary(
                ORG_ID, YearMonth.of(2026, 1), 10, 5000L, new BigDecimal("0.09000000"), -1, false));

    mockMvc
        .perform(get("/api/billing/usage").param("month", "2026-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orgId").value(ORG_ID.toString()))
        .andExpect(jsonPath("$.month").value("2026-01"))
        .andExpect(jsonPath("$.totalTasks").value(10))
        .andExpect(jsonPath("$.totalTokens").value(5000))
        .andExpect(jsonPath("$.planLimit").value(-1))
        .andExpect(jsonPath("$.limitReached").value(false));
  }

  @Test
  void GET_usage_shows_limit_reached_for_free_plan() throws Exception {
    when(usageQueryService.getMonthlySummary(any(), any()))
        .thenReturn(summary(ORG_ID, YearMonth.of(2026, 1), 50, 100000L, BigDecimal.ZERO, 50, true));

    mockMvc
        .perform(get("/api/billing/usage").param("month", "2026-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.planLimit").value(50))
        .andExpect(jsonPath("$.limitReached").value(true));
  }

  @Test
  void GET_usage_returns_400_for_invalid_month_format() throws Exception {
    mockMvc
        .perform(get("/api/billing/usage").param("month", "not-a-month"))
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

  private HandlerMethodArgumentResolver principalResolver(AuthenticatedUser user) {
    return new HandlerMethodArgumentResolver() {
      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
            && parameter.getParameterType().isAssignableFrom(AuthenticatedUser.class);
      }

      @Override
      public Object resolveArgument(
          MethodParameter parameter,
          ModelAndViewContainer mavContainer,
          NativeWebRequest webRequest,
          WebDataBinderFactory binderFactory) {
        return user;
      }
    };
  }
}
