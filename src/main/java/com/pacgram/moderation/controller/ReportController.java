package com.pacgram.moderation.controller;

import com.pacgram.common.api.ApiResponse;
import com.pacgram.common.security.AccountPrincipal;
import com.pacgram.moderation.dto.CreateReportRequest;
import com.pacgram.moderation.dto.ReportReceiptDto;
import com.pacgram.moderation.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Moderation reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report an account or its message, post, or comment")
    public ApiResponse<ReportReceiptDto> create(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody CreateReportRequest request) {
        return ApiResponse.success(reportService.create(principal, request));
    }

    @PostMapping(value = "/with-evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a report with optional screenshot or screen recording evidence")
    public ApiResponse<ReportReceiptDto> createWithEvidence(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam Long reportedUserId,
            @RequestParam String reason,
            @RequestParam(required = false) String details,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false) List<MultipartFile> evidence) {
        CreateReportRequest request = new CreateReportRequest(
                reportedUserId, reason, details, contentType, contentId);
        return ApiResponse.success(reportService.create(principal, request,
                evidence == null ? List.of() : evidence));
    }

    @GetMapping("/{reportId}/evidence/{evidenceId}")
    @Operation(summary = "Read private report evidence as its reporter or an administrator")
    public ResponseEntity<Resource> evidence(
            @PathVariable Long reportId,
            @PathVariable Long evidenceId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        Resource resource = reportService.evidenceFile(reportId, evidenceId, principal);
        String contentType = reportService.evidence(reportId, principal).stream()
                .filter(item -> item.id().equals(evidenceId))
                .findFirst().orElseThrow().contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
