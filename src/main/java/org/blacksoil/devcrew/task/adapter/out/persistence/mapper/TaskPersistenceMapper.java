package org.blacksoil.devcrew.task.adapter.out.persistence.mapper;

import org.blacksoil.devcrew.task.adapter.out.persistence.entity.TaskEntity;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskPersistenceMapper {

  TaskModel toModel(TaskEntity entity);

  TaskEntity toEntity(TaskModel model);
}
