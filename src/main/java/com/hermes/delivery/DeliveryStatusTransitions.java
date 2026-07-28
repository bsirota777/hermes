package com.hermes.delivery;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class DeliveryStatusTransitions {

    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED = new EnumMap<>(DeliveryStatus.class);

    static {
        ALLOWED.put(DeliveryStatus.CREATED, EnumSet.of(DeliveryStatus.ASSIGNED, DeliveryStatus.CANCELLED));
        ALLOWED.put(DeliveryStatus.ASSIGNED, EnumSet.of(DeliveryStatus.IN_TRANSIT, DeliveryStatus.CANCELLED));
        ALLOWED.put(DeliveryStatus.IN_TRANSIT, EnumSet.of(DeliveryStatus.DELIVERED)); // no CANCELLED here
        ALLOWED.put(DeliveryStatus.DELIVERED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED.put(DeliveryStatus.CANCELLED, EnumSet.noneOf(DeliveryStatus.class));
    }

    private DeliveryStatusTransitions() {}

    public static boolean isAllowed(DeliveryStatus from, DeliveryStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }
}