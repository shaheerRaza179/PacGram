package com.pacgram.moderation.dto;

public record ReportEvidenceDto(Long id, String fileName, String contentType, long fileSize) {
}
