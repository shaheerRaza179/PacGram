package com.pacgram.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class Pageables {
    private Pageables() {
    }

    public static Pageable withoutSort(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
