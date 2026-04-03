package org.blacksoil.devcrew.billing.adapter.out.persistence.mapper;

import org.blacksoil.devcrew.billing.adapter.out.persistence.entity.UsageRecordEntity;
import org.blacksoil.devcrew.billing.domain.UsageRecordModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsageRecordPersistenceMapper {

  UsageRecordModel toModel(UsageRecordEntity entity);

  UsageRecordEntity toEntity(UsageRecordModel model);
}
