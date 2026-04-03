package org.blacksoil.devcrew.audit.adapter.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.adapter.in.web.dto.AuditResponse;
import org.blacksoil.devcrew.audit.adapter.in.web.mapper.AuditWebMapper;
import org.blacksoil.devcrew.audit.app.service.query.AuditQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

  private final AuditQueryService auditQueryService;
  private final AuditWebMapper auditWebMapper;

  @GetMapping
  public List<AuditResponse> getAuditEvents(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
      @RequestParam(required = false) UUID projectId) {
    var events =
        projectId != null
            ? auditQueryService.findByProjectId(projectId, from, to)
            : auditQueryService.findByTimestampBetween(from, to);
    return events.stream().map(auditWebMapper::toResponse).toList();
  }
}
