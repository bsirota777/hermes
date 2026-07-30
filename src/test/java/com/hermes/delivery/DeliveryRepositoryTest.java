package com.hermes.delivery;

import com.hermes.TestcontainersConfig;
import com.hermes.user.*;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class DeliveryRepositoryTest {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SenderProfileRepository senderProfileRepository;

    @Autowired
    private RecipientProfileRepository recipientProfileRepository;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    private final Pageable pageable = PageRequest.of(0, 10);

    private User persistUser(String email) {
        return userRepository.save(new User("Test User", email, "secret"));
    }

    private SenderProfile persistSender(String email) {
        User user = persistUser(email);
        SenderProfile sender = new SenderProfile();
        sender.setUser(user);
        sender.setAddress("1 Sender St");
        sender.setPhoneNumber("0400000001");
        return senderProfileRepository.save(sender);
    }

    private RecipientProfile persistRecipient(String email) {
        User user = persistUser(email);
        RecipientProfile recipient = new RecipientProfile();
        recipient.setUser(user);
        recipient.setAddress("1 Recipient St");
        recipient.setPhoneNumber("0400000002");
        return recipientProfileRepository.save(recipient);
    }

    private DriverProfile persistDriver(String email) {
        User user = persistUser(email);
        DriverProfile driver = new DriverProfile();
        driver.setUser(user);
        driver.setLicenceNumber("DL999");
        driver.setVehiclePlate("XYZ999");
        driver.setAddress("1 Driver St");
        driver.setPhoneNumber("0400000003");
        return driverProfileRepository.save(driver);
    }

    private Delivery buildDelivery(SenderProfile sender, RecipientProfile recipient, DriverProfile driver, DeliveryStatus status) {
        Delivery delivery = new Delivery();
        delivery.setSender(sender);
        delivery.setRecipient(recipient);
        delivery.setDriver(driver);
        delivery.setStatus(status);
        delivery.setPickUpAddress("123 Test St");
        delivery.setDropOffAddress("456 Sample Ave");
        delivery.setDeliveryFee(new BigDecimal("25.00"));       // adjust field name/type to match your entity
        delivery.setDriverCommissionRate(new BigDecimal("0.20")); // adjust to match your entity
        return delivery;
    }

    @Test
    void findByDriverId_returnsDeliveriesForDriver() {
        SenderProfile sender = persistSender("sender1@example.com");
        RecipientProfile recipient = persistRecipient("recipient1@example.com");
        DriverProfile driver = persistDriver("driver1@example.com");
        deliveryRepository.save(buildDelivery(sender, recipient, driver, DeliveryStatus.ASSIGNED));

        Page<Delivery> result = deliveryRepository.findByDriverId(driver.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDriver().getId()).isEqualTo(driver.getId());
    }

    @Test
    void findBySenderId_returnsDeliveriesForSender() {
        SenderProfile sender = persistSender("sender2@example.com");
        RecipientProfile recipient = persistRecipient("recipient2@example.com");
        deliveryRepository.save(buildDelivery(sender, recipient, null, DeliveryStatus.CREATED));

        Page<Delivery> result = deliveryRepository.findBySenderId(sender.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSender().getId()).isEqualTo(sender.getId());
    }

    @Test
    void findByRecipientId_returnsDeliveriesForRecipient() {
        SenderProfile sender = persistSender("sender3@example.com");
        RecipientProfile recipient = persistRecipient("recipient3@example.com");
        deliveryRepository.save(buildDelivery(sender, recipient, null, DeliveryStatus.CREATED));

        Page<Delivery> result = deliveryRepository.findByRecipientId(recipient.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRecipient().getId()).isEqualTo(recipient.getId());
    }

    @Test
    void findByStatus_returnsMatchingDeliveries() {
        SenderProfile sender = persistSender("sender4@example.com");
        RecipientProfile recipient = persistRecipient("recipient4@example.com");
        deliveryRepository.save(buildDelivery(sender, recipient, null, DeliveryStatus.IN_TRANSIT));

        Page<Delivery> result = deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
    }

    @Test
    void findByDriverIsNull_returnsUnassignedDeliveries() {
        SenderProfile sender = persistSender("sender5@example.com");
        RecipientProfile recipient = persistRecipient("recipient5@example.com");
        deliveryRepository.save(buildDelivery(sender, recipient, null, DeliveryStatus.CREATED));

        Page<Delivery> result = deliveryRepository.findByDriverIsNull(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDriver()).isNull();
    }
}