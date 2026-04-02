package org.blacksoil.devcrew.organization.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectStore {
    ProjectModel save(ProjectModel project);
    Optional<ProjectModel> findById(UUID id);
    List<ProjectModel> findByOrgId(UUID orgId);
}
