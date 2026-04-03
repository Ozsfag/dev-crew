package org.blacksoil.devcrew.organization.adapter.out.persistence.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.organization.adapter.out.persistence.mapper.ProjectPersistenceMapper;
import org.blacksoil.devcrew.organization.adapter.out.persistence.repository.ProjectRepository;
import org.blacksoil.devcrew.organization.domain.ProjectModel;
import org.blacksoil.devcrew.organization.domain.ProjectStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProjectJpaStore implements ProjectStore {

  private final ProjectRepository repository;
  private final ProjectPersistenceMapper mapper;

  @Override
  @Transactional
  public ProjectModel save(ProjectModel project) {
    return mapper.toModel(repository.save(mapper.toEntity(project)));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ProjectModel> findById(UUID id) {
    return repository.findById(id).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectModel> findByOrgId(UUID orgId) {
    return repository.findByOrgId(orgId).stream().map(mapper::toModel).toList();
  }
}
