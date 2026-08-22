package com.hermes.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="driver_profiles") @Getter @Setter
public class DriverProfile extends ContactProfile {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id", nullable=false, unique=true) private Long userId;
    @Column(name="licence_number", nullable=false) private String licenceNumber;
    @Column(name="vehicle_plate", nullable=false) private String vehiclePlate;
    @Column private Double latitude;
    @Column private Double longitude;
}
