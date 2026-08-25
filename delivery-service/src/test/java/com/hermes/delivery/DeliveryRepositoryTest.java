package com.hermes.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class DeliveryRepositoryTest {

    @Autowired
    private DeliveryRepository deliveryRepository;

    private final Pageable pageable = PageRequest.of(0, 10);

    private Delivery buildDelivery(Long senderId, Long recipientId, Long driverId, DeliveryStatus status) {
        Delivery delivery = new Delivery();
        delivery.setSenderId(senderId);
        delivery.setRecipientId(recipientId);
        delivery.setDriverId(driverId);
        delivery.setStatus(status);
        delivery.setPickUpAddress(new Address("123", "Test St", "Springfield", "VIC", "3000"));
        delivery.setDropOffAddress(new Address("456", "Sample Ave", "Shelbyville", "VIC", "3001"));
        delivery.setDeliveryFee(new BigDecimal("25.00"));
        delivery.setDriverCommissionRate(new BigDecimal("0.80"));
        delivery.setQrCodeToken(UUID.randomUUID().toString());
        return delivery;
    }

    @Test
    void findByDriverId_returnsDeliveriesForDriver() {
        deliveryRepository.save(buildDelivery(1L, 2L, 50L, DeliveryStatus.ASSIGNED));

        Page<Delivery> result = deliveryRepository.findByDriverId(50L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDriverId()).isEqualTo(50L);
    }

    @Test
    void findBySenderId_returnsDeliveriesForSender() {
        deliveryRepository.save(buildDelivery(1L, 2L, null, DeliveryStatus.CREATED));

        Page<Delivery> result = deliveryRepository.findBySenderId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSenderId()).isEqualTo(1L);
    }

    @Test
    void findByRecipientId_returnsDeliveriesForRecipient() {
        deliveryRepository.save(buildDelivery(1L, 2L, null, DeliveryStatus.CREATED));

        Page<Delivery> result = deliveryRepository.findByRecipientId(2L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRecipientId()).isEqualTo(2L);
    }

    @Test
    void findByStatus_returnsMatchingDeliveries() {
        deliveryRepository.save(buildDelivery(1L, 2L, null, DeliveryStatus.IN_TRANSIT));

        Page<Delivery> result = deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
    }

    @Test
    void findByDriverIdIsNull_returnsUnassignedDeliveries() {
        deliveryRepository.save(buildDelivery(1L, 2L, null, DeliveryStatus.CREATED));

        Page<Delivery> result = deliveryRepository.findByDriverIdIsNull(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDriverId()).isNull();
    }

    @Test
    void countBySenderId_countsOnlyThatSendersDeliveries() {
        deliveryRepository.save(buildDelivery(1L, 2L, null, DeliveryStatus.CREATED));
        deliveryRepository.save(buildDelivery(1L, 3L, null, DeliveryStatus.CREATED));
        deliveryRepository.save(buildDelivery(9L, 3L, null, DeliveryStatus.CREATED));

        assertThat(deliveryRepository.countBySenderId(1L)).isEqualTo(2);
    }
}
