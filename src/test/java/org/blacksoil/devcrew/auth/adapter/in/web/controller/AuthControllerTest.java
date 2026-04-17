package org.blacksoil.devcrew.auth.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.blacksoil.devcrew.auth.adapter.in.web.mapper.AuthWebMapper;
import org.blacksoil.devcrew.auth.app.service.AuthService;
import org.blacksoil.devcrew.auth.app.service.AuthService.LoginResult;
import org.blacksoil.devcrew.auth.app.service.AuthService.RefreshResult;
import org.blacksoil.devcrew.auth.domain.AuthException;
import org.blacksoil.devcrew.common.exception.ConflictException;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock private AuthService authService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller = new AuthController(authService, Mappers.getMapper(AuthWebMapper.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void POST_register_returns_201_with_tokens() throws Exception {
    when(authService.register(anyString(), anyString(), any()))
        .thenReturn(new LoginResult("access-token", "refresh-token", 3600L));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"admin@test.com","password":"secret123"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value("access-token"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.expiresIn").value(3600));
  }

  @Test
  void POST_register_returns_409_when_email_taken() throws Exception {
    when(authService.register(anyString(), anyString(), any()))
        .thenThrow(new ConflictException("Email занят"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"dup@test.com","password":"pass"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void POST_register_returns_400_when_email_blank() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"","password":"pass"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void POST_login_returns_200_with_tokens() throws Exception {
    when(authService.login(anyString(), anyString()))
        .thenReturn(new LoginResult("at", "rt", 3600L));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"user@test.com","password":"secret"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("at"));
  }

  @Test
  void POST_login_returns_401_on_bad_credentials() throws Exception {
    when(authService.login(anyString(), anyString()))
        .thenThrow(new AuthException("Неверные данные"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"user@test.com","password":"wrong"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void POST_refresh_returns_200_with_new_access_token() throws Exception {
    when(authService.refresh("my-refresh-token"))
        .thenReturn(new RefreshResult("new-access", 3600L));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"my-refresh-token"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new-access"))
        .andExpect(jsonPath("$.expiresIn").value(3600));
  }

  @Test
  void POST_refresh_returns_401_when_token_invalid() throws Exception {
    when(authService.refresh(anyString())).thenThrow(new AuthException("Токен недействителен"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"bad-token"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void POST_logout_returns_204_on_success() throws Exception {
    doNothing().when(authService).logout(anyString());

    mockMvc
        .perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"my-refresh-token"}
                    """))
        .andExpect(status().isNoContent());
  }

  @Test
  void POST_logout_returns_401_when_token_not_found() throws Exception {
    doThrow(new AuthException("Токен не найден")).when(authService).logout(anyString());

    mockMvc
        .perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":"unknown-token"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void POST_logout_returns_400_when_token_blank() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"refreshToken":""}
                    """))
        .andExpect(status().isBadRequest());
  }
}
