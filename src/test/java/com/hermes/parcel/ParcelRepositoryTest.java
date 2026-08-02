package com.hermes.parcel;

import com.hermes.TestcontainersConfig;
import com.hermes.delivery.Delivery;
import com.hermes.delivery.DeliveryRepository;
import com.hermes.delivery.DeliveryStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class ParcelRepositoryTest {

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SenderProfileRepository senderProfileRepository;

    @Autowired
    private RecipientProfileRepository recipientProfileRepository;

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

    private Delivery persistDelivery(String senderEmail, String recipientEmail) {
        SenderProfile sender = persistSender(senderEmail);
        RecipientProfile recipient = persistRecipient(recipientEmail);
        Delivery delivery = new Delivery();
        delivery.setSender(sender);
        delivery.setRecipient(recipient);
        delivery.setPickUpAddress("123 Test St");
        delivery.setDropOffAddress("456 Sample Ave");
        delivery.setDeliveryFee(new BigDecimal("25.00"));       // adjust field name/type to match your entity
        delivery.setDriverCommissionRate(new BigDecimal("0.20")); // adjust to match your entity
        delivery.setStatus(DeliveryStatus.CREATED);
        return deliveryRepository.save(delivery);
    }

    private Parcel buildParcel(Delivery delivery, boolean insured) {
        Parcel parcel = new Parcel();
        parcel.setDelivery(delivery);
        parcel.setDescription("Test parcel");
        parcel.setLengthCm(new BigDecimal("30.00"));
        parcel.setWidthCm(new BigDecimal("20.00"));
        parcel.setHeightCm(new BigDecimal("15.00"));
        parcel.setWeightKg(new BigDecimal("2.50"));
        parcel.setDeclaredValue(new BigDecimal("100.00"));
        parcel.setInsured(insured);
        if (insured) {
            parcel.setInsuredValue(new BigDecimal("100.00"));
        }
        return parcel;
    }

    @Test
    void findByDeliveryId_returnsAllParcelsForDelivery() {
        Delivery delivery = persistDelivery("sender1@example.com", "recipient1@example.com");
        parcelRepository.save(buildParcel(delivery, false));
        parcelRepository.save(buildParcel(delivery, false));

        List<Parcel> parcels = parcelRepository.findByDeliveryId(delivery.getId());

        assertThat(parcels).hasSize(2);
        assertThat(parcels).allMatch(p -> p.getDelivery().getId().equals(delivery.getId()));
    }

    @Test
    void findByDeliveryId_withPageable_returnsPagedParcels() {
        Delivery delivery = persistDelivery("sender2@example.com", "recipient2@example.com");
        parcelRepository.save(buildParcel(delivery, false));
        parcelRepository.save(buildParcel(delivery, false));

        Page<Parcel> page = parcelRepository.findByDeliveryId(delivery.getId(), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void countByDeliveryId_returnsCorrectCount() {
        Delivery delivery = persistDelivery("sender3@example.com", "recipient3@example.com");
        parcelRepository.save(buildParcel(delivery, false));
        parcelRepository.save(buildParcel(delivery, false));
        parcelRepository.save(buildParcel(delivery, false));

        long count = parcelRepository.countByDeliveryId(delivery.getId());

        assertThat(count).isEqualTo(3);
    }

    @Test
    void countByDeliveryId_returnsZero_whenNoParcels() {
        Delivery delivery = persistDelivery("sender4@example.com", "recipient4@example.com");

        long count = parcelRepository.countByDeliveryId(delivery.getId());

        assertThat(count).isZero();
    }

    @Test
    void findByInsuredTrue_returnsOnlyInsuredParcels() {
        Delivery delivery = persistDelivery("sender5@example.com", "recipient5@example.com");
        parcelRepository.save(buildParcel(delivery, true));
        parcelRepository.save(buildParcel(delivery, false));

        Page<Parcel> page = parcelRepository.findByInsuredTrue(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).isInsured()).isTrue();
    }
}