package com.hermes.user.client;

public interface DeliveryServiceClient {
    DeliveryCounts getCountsForUser(Long userId);

    record DeliveryCounts(long sentCount, long receivedCount) {}
}
