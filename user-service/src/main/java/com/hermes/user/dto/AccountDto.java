package com.hermes.user.dto;

import com.hermes.user.Role;

import java.time.Instant;

public record AccountDto(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {}
