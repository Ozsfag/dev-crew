package org.blacksoil.devcrew.billing.adapter.in.web.mapper;

import org.blacksoil.devcrew.billing.adapter.in.web.dto.UsageSummaryResponse;
import org.blacksoil.devcrew.billing.domain.UsageSummaryModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BillingWebMapper {

  @Mapping(target = "orgId", expression = "java(model.orgId().toString())")
  @Mapping(target = "month", expression = "java(model.month().toString())")
  @Mapping(target = "totalCostUsd", expression = "java(model.totalCostUsd().toPlainString())")
  UsageSummaryResponse toResponse(UsageSummaryModel model);
}
