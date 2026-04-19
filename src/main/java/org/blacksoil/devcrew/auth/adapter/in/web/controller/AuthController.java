package org.blacksoil.devcrew.auth.adapter.in.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.auth.adapter.in.web.dto.*;
import org.blacksoil.devcrew.auth.adapter.in.web.mapper.AuthWebMapper;
import org.blacksoil.devcrew.auth.app.service.AuthRateLimitService;
import org.blacksoil.devcrew.auth.app.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final AuthRateLimitService rateLimitService;
  private final AuthWebMapper mapper;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
    rateLimitService.checkRegisterAttempt(request.email());
    return mapper.toResponse(
        authService.register(request.email(), request.password(), request.orgName()));
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    rateLimitService.checkLoginAttempt(request.email());
    return mapper.toResponse(authService.login(request.email(), request.password()));
  }

  @PostMapping("/refresh")
  public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return mapper.toResponse(authService.refresh(request.refreshToken()));
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request.refreshToken());
  }
}
