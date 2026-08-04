package com.hermes.delivery;

import com.hermes.parcel.Parcel;
import com.hermes.user.Address;
import com.hermes.user.DriverProfile;
import com.hermes.user.RecipientProfile;
import com.hermes.user.SenderProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "streetNumber", column = @Column(name = "pick_up_street_number", nullable = false)),
            @AttributeOverride(name = "streetName", column = @Column(name = "pick_up_street_name", nullable = false)),
            @AttributeOverride(name = "suburb", column = @Column(name = "pick_up_suburb", nullable = false)),
            @AttributeOverride(name = "state", column = @Column(name = "pick_up_state", nullable = false)),
            @AttributeOverride(name = "postcode", column = @Column(name = "pick_up_postcode", nullable = false))
    })
    private Address pickUpAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "streetNumber", column = @Column(name = "drop_off_street_number", nullable = false)),
            @AttributeOverride(name = "streetName", column = @Column(name = "drop_off_street_name", nullable = false)),
            @AttributeOverride(name = "suburb", column = @Column(name = "drop_off_suburb", nullable = false)),
            @AttributeOverride(name = "state", column = @Column(name = "drop_off_state", nullable = false)),
            @AttributeOverride(name = "postcode", column = @Column(name = "drop_off_postcode", nullable = false))
    })
    private Address dropOffAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    // Stored per-delivery rather than a single global constant, so you can change
    // the platform's cut over time without affecting already-created deliveries
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal driverCommissionRate; // e.g. 0.8000 = driver keeps 80%

    @Column(name = "pick_up_latitude")
    private Double pickUpLatitude;

    @Column(name = "pick_up_longitude")
    private Double pickUpLongitude;

    @Column(name = "drop_off_latitude")
    private Double dropOffLatitude;

    @Column(name = "drop_off_longitude")
    private Double dropOffLongitude;

    @Version
    private Long version; // optimistic locking for the reserve race condition
}