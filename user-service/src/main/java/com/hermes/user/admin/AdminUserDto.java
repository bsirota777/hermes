package com.hermes.user.admin;

import com.hermes.user.Role;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String name,
        String email,
        Role role,
        boolean banned,
        long sentCount,
        long receivedCount,
        Instant createdAt
) {}
