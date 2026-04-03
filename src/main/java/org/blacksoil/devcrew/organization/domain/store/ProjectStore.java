package org.blacksoil.devcrew.organization.domain.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;

public interface ProjectStore {
  ProjectModel save(ProjectModel project);

  Optional<ProjectModel> findById(UUID id);

  List<ProjectModel> findByOrgId(UUID orgId);
}
