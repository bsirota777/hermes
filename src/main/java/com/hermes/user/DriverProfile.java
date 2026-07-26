package com.hermes.user;

import com.hermes.delivery.Delivery;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
public class DriverProfile extends ContactProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // shared PK with User

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String licenceNumber;
    private String vehiclePlate;
    // driver-only fields

    @OneToMany(mappedBy = "driver")
    private List<Delivery> deliveriesAsDriver;
}