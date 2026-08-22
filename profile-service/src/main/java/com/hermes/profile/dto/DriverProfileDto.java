package com.hermes.profile.dto;
import com.hermes.common.address.AddressDto;
public record DriverProfileDto(Long id, Long userId, AddressDto address, String phoneNumber, String licenceNumber, String vehiclePlate, Double latitude, Double longitude) {}
