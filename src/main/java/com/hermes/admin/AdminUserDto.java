package com.hermes.admin;

import java.time.Instant;

public record AdminUserDto(
        Long id,
        String name,
        String email,
        String role,
        boolean banned,
        long sentCount,
        long receivedCount,
        Instant createdAt
) {}