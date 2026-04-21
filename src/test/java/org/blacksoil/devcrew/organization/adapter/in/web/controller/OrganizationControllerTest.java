package org.blacksoil.devcrew.organization.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.blacksoil.devcrew.bootstrap.AuthenticatedUser;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.blacksoil.devcrew.organization.adapter.in.web.mapper.OrganizationWebMapper;
import org.blacksoil.devcrew.organization.app.service.command.OrganizationCommandService;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

  @Mock private OrganizationCommandService commandService;
  @Mock private OrganizationQueryService queryService;

  private MockMvc mockMvc;

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
  private static final UUID ORG_ID = UUID.randomUUID();
  private static final AuthenticatedUser CURRENT_USER =
      new AuthenticatedUser(UUID.randomUUID(), ORG_ID, UserRole.ARCHITECT);

  @BeforeEach
  void setUp() {
    var controller =
        new OrganizationController(
            commandService, queryService, Mappers.getMapper(OrganizationWebMapper.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(principalResolver(CURRENT_USER))
            .build();
  }

  @Test
  void POST_organizations_returns_201_with_org() throws Exception {
    when(commandService.createOrganization("Acme Corp")).thenReturn(org(ORG_ID, "Acme Corp"));

    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme Corp\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Acme Corp"))
        .andExpect(jsonPath("$.plan").value("FREE"));
  }

  @Test
  void POST_organizations_returns_400_when_name_blank() throws Exception {
    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void GET_organizations_id_returns_200_when_found() throws Exception {
    // id совпадает с ORG_ID текущего пользователя — доступ разрешён
    when(queryService.getById(ORG_ID)).thenReturn(org(ORG_ID, "Acme Corp"));

    mockMvc
        .perform(get("/api/organizations/{id}", ORG_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ORG_ID.toString()));
  }

  @Test
  void GET_organizations_id_returns_403_when_wrong_org() throws Exception {
    var otherId = UUID.randomUUID();

    mockMvc.perform(get("/api/organizations/{id}", otherId)).andExpect(status().isForbidden());
  }

  @Test
  void GET_organizations_id_returns_404_when_not_found() throws Exception {
    when(queryService.getById(ORG_ID)).thenThrow(new NotFoundException("Organization", ORG_ID));

    mockMvc.perform(get("/api/organizations/{id}", ORG_ID)).andExpect(status().isNotFound());
  }

  @Test
  void POST_organizations_orgId_projects_returns_201() throws Exception {
    // orgId совпадает с ORG_ID текущего пользователя — доступ разрешён
    when(commandService.createProject(eq(ORG_ID), eq("Backend"), eq("/repos/backend")))
        .thenReturn(project(ORG_ID));

    mockMvc
        .perform(
            post("/api/organizations/{orgId}/projects", ORG_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Backend\",\"repoPath\":\"/repos/backend\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Backend"));
  }

  @Test
  void POST_organizations_orgId_projects_returns_403_when_wrong_org() throws Exception {
    var otherOrgId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/organizations/{orgId}/projects", otherOrgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Backend\",\"repoPath\":\"/repos/backend\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_organizations_orgId_projects_returns_400_when_name_blank() throws Exception {
    mockMvc
        .perform(
            post("/api/organizations/{orgId}/projects", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void POST_organizations_orgId_projects_returns_400_when_repoPath_traversal() throws Exception {
    mockMvc
        .perform(
            post("/api/organizations/{orgId}/projects", ORG_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Backend\",\"repoPath\":\"../../etc/passwd\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void GET_organizations_orgId_projects_uses_currentUser_orgId() throws Exception {
    // когда currentUser != null — используется orgId из principal, не из path
    when(queryService.getProjectsByOrg(ORG_ID)).thenReturn(List.of(project(ORG_ID)));

    mockMvc
        .perform(get("/api/organizations/{orgId}/projects", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    verify(queryService).getProjectsByOrg(ORG_ID);
  }

  @Test
  void GET_organizations_me_returns_current_users_org() throws Exception {
    when(queryService.getById(ORG_ID)).thenReturn(org(ORG_ID, "Acme Corp"));

    mockMvc
        .perform(get("/api/organizations/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ORG_ID.toString()));
  }

  private OrganizationModel org(UUID id, String name) {
    return new OrganizationModel(id, name, OrgPlan.FREE, null, NOW, NOW);
  }

  private ProjectModel project(UUID orgId) {
    return new ProjectModel(UUID.randomUUID(), orgId, "Backend", "/repos/backend", NOW, NOW);
  }

  /** Resolver который подставляет фиксированный AuthenticatedUser в @AuthenticationPrincipal. */
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
