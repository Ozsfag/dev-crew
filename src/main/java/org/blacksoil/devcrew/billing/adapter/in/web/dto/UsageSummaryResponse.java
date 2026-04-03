package org.blacksoil.devcrew.billing.adapter.in.web.dto;

public record UsageSummaryResponse(
    String orgId,
    String month,
    int totalTasks,
    long totalTokens,
    String totalCostUsd,
    int planLimit,
    boolean limitReached) {}
