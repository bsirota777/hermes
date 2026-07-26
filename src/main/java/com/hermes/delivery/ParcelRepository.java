package com.hermes.delivery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    // A delivery can have multiple parcels
    List<Parcel> findByDeliveryId(Long deliveryId);

    Page<Parcel> findByDeliveryId(Long deliveryId, Pageable pageable);

    long countByDeliveryId(Long deliveryId);

    Page<Parcel> findByInsuredTrue(Pageable pageable);
}