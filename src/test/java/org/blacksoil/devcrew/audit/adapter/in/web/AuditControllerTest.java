package org.blacksoil.devcrew.audit.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.audit.adapter.in.web.mapper.AuditWebMapper;
import org.blacksoil.devcrew.audit.app.service.query.AuditQueryService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.blacksoil.devcrew.common.PageResult;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private AuditQueryService auditQueryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller =
        new AuditController(auditQueryService, Mappers.getMapper(AuditWebMapper.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void GET_audit_returns_200_with_events() throws Exception {
    var event =
        new AuditEventModel(
            UUID.randomUUID(),
            null,
            null,
            "system",
            "TASK_COMPLETED",
            UUID.randomUUID(),
            "details",
            NOW);
    var pageResult = new PageResult<>(List.of(event), 0, 20, 1);
    when(auditQueryService.findByTimestampBetween(any(), any(), eq(0), eq(20)))
        .thenReturn(pageResult);

    mockMvc
        .perform(
            get("/api/audit")
                .param("from", "2024-01-01T00:00:00Z")
                .param("to", "2024-12-31T23:59:59Z")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].action").value("TASK_COMPLETED"))
        .andExpect(jsonPath("$.content[0].actorEmail").value("system"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void GET_audit_returns_empty_page_when_no_events() throws Exception {
    when(auditQueryService.findByTimestampBetween(any(), any(), eq(0), eq(20)))
        .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

    mockMvc
        .perform(
            get("/api/audit")
                .param("from", "2024-01-01T00:00:00Z")
                .param("to", "2024-01-02T00:00:00Z")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void GET_audit_with_projectId_calls_findByProjectId() throws Exception {
    var projectId = UUID.randomUUID();
    var event =
        new AuditEventModel(
            UUID.randomUUID(),
            projectId,
            null,
            "system",
            "TASK_COMPLETED",
            UUID.randomUUID(),
            "details",
            Instant.parse("2026-01-01T10:00:00Z"));
    var pageResult = new PageResult<>(List.of(event), 0, 20, 1);
    when(auditQueryService.findByProjectId(eq(projectId), any(), any(), eq(0), eq(20)))
        .thenReturn(pageResult);

    mockMvc
        .perform(
            get("/api/audit")
                .param("from", "2024-01-01T00:00:00Z")
                .param("to", "2024-12-31T23:59:59Z")
                .param("projectId", projectId.toString())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));

    verify(auditQueryService).findByProjectId(eq(projectId), any(), any(), eq(0), eq(20));
    verify(auditQueryService, never()).findByTimestampBetween(any(), any(), eq(0), eq(20));
  }

  @Test
  void GET_audit_returns_400_when_from_missing() throws Exception {
    mockMvc
        .perform(
            get("/api/audit")
                .param("to", "2024-12-31T23:59:59Z")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
