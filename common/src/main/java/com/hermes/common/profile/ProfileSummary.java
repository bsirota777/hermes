package com.hermes.common.profile;

public record ProfileSummary(
        Long profileId,
        Long userId,
        String name
) {}