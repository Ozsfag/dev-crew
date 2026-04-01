package org.blacksoil.devcrew.auth.adapter.in.web;

import org.blacksoil.devcrew.auth.adapter.in.web.dto.LoginResponse;
import org.blacksoil.devcrew.auth.adapter.in.web.dto.RefreshResponse;
import org.blacksoil.devcrew.auth.app.service.AuthService.LoginResult;
import org.blacksoil.devcrew.auth.app.service.AuthService.RefreshResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthWebMapper {

    LoginResponse toResponse(LoginResult result);

    RefreshResponse toResponse(RefreshResult result);
}
