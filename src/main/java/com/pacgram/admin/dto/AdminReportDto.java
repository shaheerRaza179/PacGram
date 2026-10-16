package com.pacgram.admin.dto;

import com.pacgram.moderation.dto.ReportEvidenceDto;

import java.time.Instant;
import java.util.List;

public record AdminReportDto(
        Long id,
        Long reporterId,
        String reporterUsername,
        String reporterName,
        Long reportedUserId,
        String reportedUsername,
        String reportedName,
        String reason,
        String details,
        String contentType,
        Long contentId,
        String contentSnapshot,
        String status,
        String resolutionNote,
        Instant createdAt,
        Instant updatedAt,
        List<ReportEvidenceDto> evidence) {
}
