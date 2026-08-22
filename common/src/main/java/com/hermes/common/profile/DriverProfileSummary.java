package com.hermes.common.profile;

public record DriverProfileSummary(
        Long profileId,
        Long userId,
        String name,
        Double latitude,
        Double longitude
) {}