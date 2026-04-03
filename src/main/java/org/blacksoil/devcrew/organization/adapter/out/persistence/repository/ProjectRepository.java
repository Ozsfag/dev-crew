package org.blacksoil.devcrew.organization.adapter.out.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.organization.adapter.out.persistence.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
  List<ProjectEntity> findByOrgId(UUID orgId);
}
