package org.blacksoil.devcrew.organization.adapter.out.persistence.store;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.organization.adapter.out.persistence.mapper.OrganizationPersistenceMapper;
import org.blacksoil.devcrew.organization.adapter.out.persistence.repository.OrganizationRepository;
import org.blacksoil.devcrew.organization.domain.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.OrganizationStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrganizationJpaStore implements OrganizationStore {

  private final OrganizationRepository repository;
  private final OrganizationPersistenceMapper mapper;

  @Override
  @Transactional
  public OrganizationModel save(OrganizationModel organization) {
    return mapper.toModel(repository.save(mapper.toEntity(organization)));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<OrganizationModel> findById(UUID id) {
    return repository.findById(id).map(mapper::toModel);
  }
}
