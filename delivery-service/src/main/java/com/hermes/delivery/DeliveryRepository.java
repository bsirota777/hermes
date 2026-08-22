package com.hermes.delivery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Page<Delivery> findByDriverId(Long driverId, Pageable pageable);

    Page<Delivery> findBySenderId(Long senderId, Pageable pageable);

    Page<Delivery> findByRecipientId(Long recipientId, Pageable pageable);

    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);

    Page<Delivery> findByDriverIdIsNull(Pageable pageable);

    long countBySenderId(Long senderId);
    long countByRecipientId(Long recipientId);
}