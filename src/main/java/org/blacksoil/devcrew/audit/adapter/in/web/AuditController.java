package org.blacksoil.devcrew.audit.adapter.in.web;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.adapter.in.web.dto.AuditResponse;
import org.blacksoil.devcrew.audit.adapter.in.web.mapper.AuditWebMapper;
import org.blacksoil.devcrew.audit.app.service.query.AuditQueryService;
import org.blacksoil.devcrew.common.PageResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

  private final AuditQueryService auditQueryService;
  private final AuditWebMapper auditWebMapper;

  @GetMapping
  public PageResult<AuditResponse> getAuditEvents(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
      @RequestParam(required = false) UUID projectId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var result =
        projectId != null
            ? auditQueryService.findByProjectId(projectId, from, to, page, size)
            : auditQueryService.findByTimestampBetween(from, to, page, size);
    return new PageResult<>(
        result.content().stream().map(auditWebMapper::toResponse).toList(),
        result.page(),
        result.size(),
        result.totalElements());
  }
}
