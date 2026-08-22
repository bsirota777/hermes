package com.hermes.user.client;

import com.hermes.user.dto.DriverProfileDto;
import com.hermes.user.dto.UpdateAddressRequest;
import com.hermes.user.dto.DriverRegistrationRequest;

public interface ProfileServiceClient {
    DriverProfileDto registerDriver(Long userId, DriverRegistrationRequest request);
    DriverProfileDto updateDriver(Long userId, DriverRegistrationRequest request);
    DriverProfileDto getDriver(Long userId);
    void updateAddress(Long userId, UpdateAddressRequest request);
}
