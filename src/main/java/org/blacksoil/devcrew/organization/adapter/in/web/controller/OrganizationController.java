package org.blacksoil.devcrew.organization.adapter.in.web.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.bootstrap.AuthenticatedUser;
import org.blacksoil.devcrew.organization.adapter.in.web.dto.CreateOrganizationRequest;
import org.blacksoil.devcrew.organization.adapter.in.web.dto.CreateProjectRequest;
import org.blacksoil.devcrew.organization.adapter.in.web.dto.OrganizationResponse;
import org.blacksoil.devcrew.organization.adapter.in.web.dto.ProjectResponse;
import org.blacksoil.devcrew.organization.adapter.in.web.mapper.OrganizationWebMapper;
import org.blacksoil.devcrew.organization.app.service.command.OrganizationCommandService;
import org.blacksoil.devcrew.organization.app.service.query.OrganizationQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

  private final OrganizationCommandService commandService;
  private final OrganizationQueryService queryService;
  private final OrganizationWebMapper mapper;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
    return mapper.toResponse(commandService.createOrganization(request.name()));
  }

  @GetMapping("/{id}")
  public OrganizationResponse getById(@PathVariable UUID id) {
    return mapper.toResponse(queryService.getById(id));
  }

  @PostMapping("/{orgId}/projects")
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponse createProject(
      @PathVariable UUID orgId, @Valid @RequestBody CreateProjectRequest request) {
    return mapper.toResponse(
        commandService.createProject(orgId, request.name(), request.repoPath()));
  }

  @GetMapping("/{orgId}/projects")
  public List<ProjectResponse> getProjects(
      @SuppressWarnings("unused") @PathVariable UUID orgId,
      @AuthenticationPrincipal AuthenticatedUser currentUser) {
    // Пользователь может видеть только проекты своей организации; path variable игнорируется
    return queryService.getProjectsByOrg(currentUser.orgId()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @GetMapping("/me")
  public OrganizationResponse getMyOrganization(
      @AuthenticationPrincipal AuthenticatedUser currentUser) {
    return mapper.toResponse(queryService.getById(currentUser.orgId()));
  }
}
