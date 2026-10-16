package com.pacgram.common.event;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    MESSAGE;

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
