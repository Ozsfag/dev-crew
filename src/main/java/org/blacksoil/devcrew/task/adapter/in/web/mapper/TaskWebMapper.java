package org.blacksoil.devcrew.task.adapter.in.web.mapper;

import org.blacksoil.devcrew.task.adapter.in.web.dto.TaskResponse;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskWebMapper {

  TaskResponse toResponse(TaskModel model);
}
