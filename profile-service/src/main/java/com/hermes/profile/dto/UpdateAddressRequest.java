package com.hermes.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateAddressRequest(@NotNull @Valid AddressDto address, @NotBlank String phoneNumber) {}