package org.blacksoil.devcrew.audit.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.audit.app.service.query.AuditQueryService;
import org.blacksoil.devcrew.audit.domain.AuditEventModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping
    public List<AuditEventModel> getAuditEvents(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return auditQueryService.findByTimestampBetween(from, to);
    }
}
