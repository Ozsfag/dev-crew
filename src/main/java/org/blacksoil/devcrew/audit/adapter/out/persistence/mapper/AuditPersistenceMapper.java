package org.blacksoil.devcrew.audit.adapter.out.persistence.mapper;

import org.blacksoil.devcrew.audit.adapter.out.persistence.entity.AuditEventEntity;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditPersistenceMapper {

  AuditEventEntity toEntity(AuditEventModel model);

  AuditEventModel toModel(AuditEventEntity entity);
}
