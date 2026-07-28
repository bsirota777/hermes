package com.hermes.delivery;

import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.delivery.route.DeliveryRequestQueueHandler;
import com.hermes.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import com.hermes.delivery.dto.ParcelDto;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

@SpringBootTest
class DeliveryServiceQueueTest {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private SenderProfileRepository senderProfileRepository;

    @Autowired
    private RecipientProfileRepository recipientProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private DeliveryRequestQueueHandler deliveryRequestQueueHandler;

    private Long senderId;
    private Long recipientId;
    private Long userId;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        senderProfileRepository.deleteAll();
        recipientProfileRepository.deleteAll();
        userRepository.deleteAll();

        User senderUser = new User("jdoe", "sender.test@example.com", "secret");
        User savedSenderUser = userRepository.save(senderUser);

        User recipientUser = new User("adoe", "recipient.test@example.com", "secret");
        User savedRecipientUser = userRepository.save(recipientUser);

        SenderProfile sender = new SenderProfile();
        sender.setAddress("123 ABC street");
        sender.setPhoneNumber("0412788899");
        sender.setUser(savedSenderUser);
        senderId = senderProfileRepository.save(sender).getId();

        RecipientProfile recipient = new RecipientProfile();
        recipient.setAddress("345 XYZ street");
        recipient.setPhoneNumber("041234567");
        recipient.setUser(savedRecipientUser);
        recipientId = recipientProfileRepository.save(recipient).getId();
    }

    @Test
    void createDeliveryRequest_savesToDbAndSendsToQueue() {

        DeliveryRequestDto dto = new DeliveryRequestDto(
                senderId, recipientId, "123 Main St", "456 Oak Ave", new BigDecimal("25.00"),
                List.of(new ParcelDto(
                        "Test parcel",
                        new BigDecimal("10.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("2.00"),
                        new BigDecimal("50.00"),
                        false,
                        null
                ))
        );

        Delivery saved = deliveryService.createDeliveryRequest(dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.CREATED);
        assertThat(deliveryRepository.findById(saved.getId())).isPresent();

        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> verify(deliveryRequestQueueHandler).handle(captor.capture()));

        assertThat(captor.getValue().getId()).isEqualTo(saved.getId());
    }
}
