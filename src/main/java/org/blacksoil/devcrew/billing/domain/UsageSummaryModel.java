package org.blacksoil.devcrew.billing.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/** Агрегированный отчёт по использованию за календарный месяц. */
public record UsageSummaryModel(
    UUID orgId,
    YearMonth month,
    int totalTasks,
    long totalTokens,
    BigDecimal totalCostUsd,
    /** Лимит задач по плану. -1 означает неограниченный доступ (PRO / ENTERPRISE). */
    int planLimit,
    boolean limitReached) {}
