package com.hermes.delivery.internal;

import com.hermes.common.profile.ProfileSummary;
import com.hermes.delivery.DeliveryRepository;
import com.hermes.delivery.client.ProfileServiceClient;
import com.hermes.delivery.internal.dto.DeliveryCountsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-only endpoint used by user-service's admin dashboard to show per-user
 * sent/received delivery counts. Delivery.senderId/recipientId store profile IDs
 * (not user IDs), so this first resolves the user's sender/recipient profile via
 * profile-service, then counts deliveries against those profile IDs. Same
 * N+1-over-HTTP cost already flagged on DeliveryMapper - acceptable for an
 * admin-only, low-traffic endpoint but worth a batch endpoint later if the
 * admin user list grows large.
 */
@RestController
@RequestMapping("/internal/deliveries")
public class InternalDeliveryController {

    private final DeliveryRepository deliveryRepository;
    private final ProfileServiceClient profileServiceClient;

    public InternalDeliveryController(DeliveryRepository deliveryRepository,
                                      ProfileServiceClient profileServiceClient) {
        this.deliveryRepository = deliveryRepository;
        this.profileServiceClient = profileServiceClient;
    }

    @GetMapping("/counts/{userId}")
    public DeliveryCountsResponse getCountsForUser(@PathVariable Long userId) {
        long sentCount = profileServiceClient.findSenderProfileByUserId(userId)
                .map(ProfileSummary::profileId)
                .map(deliveryRepository::countBySenderId)
                .orElse(0L);

        long receivedCount = profileServiceClient.findRecipientProfileByUserId(userId)
                .map(ProfileSummary::profileId)
                .map(deliveryRepository::countByRecipientId)
                .orElse(0L);

        return new DeliveryCountsResponse(sentCount, receivedCount);
    }
}
