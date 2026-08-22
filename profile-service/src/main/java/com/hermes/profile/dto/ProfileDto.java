package com.hermes.profile.dto;
import com.hermes.common.address.AddressDto;
public record ProfileDto(Long id, Long userId, AddressDto address, String phoneNumber) {}
