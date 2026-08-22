package com.hermes.common.profile;

import com.hermes.common.address.AddressDto;

public record FindOrCreateProfileRequest(
        Long userId,
        AddressDto address,
        String phoneNumber
) {}