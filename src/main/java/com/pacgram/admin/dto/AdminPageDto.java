package com.pacgram.admin.dto;

import java.util.List;

public record AdminPageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> AdminPageDto<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AdminPageDto<>(content, page, size, totalElements, totalPages);
    }
}
