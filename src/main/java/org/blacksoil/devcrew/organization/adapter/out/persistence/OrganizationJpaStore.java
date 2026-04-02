package org.blacksoil.devcrew.organization.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.organization.domain.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.OrganizationStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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
