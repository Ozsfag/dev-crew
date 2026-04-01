package org.blacksoil.devcrew.task.adapter.out.persistence;

import org.blacksoil.devcrew.task.domain.TaskModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskPersistenceMapper {

    TaskModel toModel(TaskEntity entity);

    TaskEntity toEntity(TaskModel model);
}
