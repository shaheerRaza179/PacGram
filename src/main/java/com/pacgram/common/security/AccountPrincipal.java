package com.pacgram.common.security;

import java.security.Principal;

public record AccountPrincipal(Long userId, String username, boolean administrator) implements Principal {

    public AccountPrincipal(Long userId, String username) {
        this(userId, username, false);
    }

    @Override
    public String getName() {
        return username;
    }
}
