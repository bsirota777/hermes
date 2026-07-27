package com.hermes.delivery;

import com.hermes.user.DriverProfile;
import com.hermes.user.RecipientProfile;
import com.hermes.user.SenderProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @ManyToOne
    @JoinColumn(name = "sender_profile_id", nullable = false)
    private SenderProfile sender;

    @ManyToOne
    @JoinColumn(name = "driver_profile_id")
    private DriverProfile driver; // nullable until a driver is assigned

    @ManyToOne
    @JoinColumn(name = "recipient_profile_id", nullable = false)
    private RecipientProfile recipient;

    @OneToMany(mappedBy = "delivery")
    private List<Parcel> parcels = new ArrayList<>();

    @Column(name = "pick_up_address", nullable = false)
    private String pickUpAddress;

    @Column(name = "drop_off_address", nullable = false)
    private String dropOffAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    // pickup/dropoff addresses, timestamps, etc.
}

