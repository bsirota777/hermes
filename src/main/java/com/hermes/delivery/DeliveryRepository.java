package com.hermes.delivery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // Find all deliveries where a given driver profile is assigned
    Page<Delivery> findByDriverId(Long driverId, Pageable pageable);

    // Find all deliveries where a given sender profile is the sender
    Page<Delivery> findBySenderId(Long senderId, Pageable pageable);

    // Find all deliveries where a given recipient profile is the recipient
    Page<Delivery> findByRecipientId(Long recipientId, Pageable pageable);

    // Find deliveries by status (e.g. "CREATED", "IN_TRANSIT")
    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);

    // Find unassigned deliveries (no driver yet) - useful for a driver "claim job" flow
    Page<Delivery> findByDriverIsNull(Pageable pageable);

    @Query("SELECT d FROM Delivery d " +
            "JOIN FETCH d.sender s JOIN FETCH s.user " +
            "JOIN FETCH d.recipient r JOIN FETCH r.user " +
            "LEFT JOIN FETCH d.driver dr LEFT JOIN FETCH dr.user " +
            "ORDER BY d.createdAt DESC")
    Page<Delivery> findAllWithDetails(Pageable pageable);
}
