package com.hermes.profile;

import com.hermes.common.address.AddressDto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Address {
    @NotBlank @Column(name = "street_number", nullable = false) private String streetNumber;
    @NotBlank @Column(name = "street_name", nullable = false) private String streetName;
    @NotBlank @Column(nullable = false) private String suburb;
    @NotBlank @Column(nullable = false) private String state;
    @NotBlank @Column(nullable = false) private String postcode;

    public String toFormattedString() {
        return String.format("%s %s, %s %s %s, Australia", streetNumber, streetName, suburb, state, postcode);
    }
    public AddressDto toDto() { return new AddressDto(streetNumber, streetName, suburb, state, postcode); }
    public static Address from(AddressDto dto) { return new Address(dto.streetNumber(), dto.streetName(), dto.suburb(), dto.state(), dto.postcode()); }
}
