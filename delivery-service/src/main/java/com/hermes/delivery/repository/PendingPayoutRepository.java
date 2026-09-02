package com.hermes.delivery.repository;

import com.hermes.delivery.entity.PendingPayout;
import com.hermes.delivery.entity.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PendingPayoutRepository extends JpaRepository<PendingPayout, Long> {
    List<PendingPayout> findByStatus(PayoutStatus status);
}