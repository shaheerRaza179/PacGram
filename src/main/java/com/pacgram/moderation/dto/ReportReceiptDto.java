package com.pacgram.moderation.dto;

import java.time.Instant;

public record ReportReceiptDto(Long id, String status, Instant createdAt) {
}
