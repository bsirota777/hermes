package com.hermes.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAddressRequest(@NotBlank String address) {}