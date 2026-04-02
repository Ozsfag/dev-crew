package org.blacksoil.devcrew.audit.adapter.in.web.mapper;

import org.blacksoil.devcrew.audit.adapter.in.web.dto.AuditResponse;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditWebMapper {

    AuditResponse toResponse(AuditEventModel model);
}
