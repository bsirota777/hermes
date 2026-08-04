package com.hermes.delivery;

import com.hermes.TestcontainersConfig;
import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.parcel.dto.ParcelDto;
import com.hermes.pricing.PricingService;
import com.hermes.delivery.route.DeliveryRequestQueueHandler;
import com.hermes.geocoding.Coordinates;
import com.hermes.geocoding.GeocodingService;
import com.hermes.user.*;
import com.hermes.user.dto.AddressDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
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

    @MockitoBean
    private GeocodingService geocodingService;

    @MockitoBean
    private PricingService pricingService;

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
        sender.setAddress(new Address("123", "ABC Street", "Springfield", "VIC", "3000"));
        sender.setPhoneNumber("0412788899");
        sender.setUser(savedSenderUser);
        senderId = senderProfileRepository.save(sender).getId();

        RecipientProfile recipient = new RecipientProfile();
        recipient.setAddress(new Address("345", "XYZ Street", "Shelbyville", "VIC", "3001"));
        recipient.setPhoneNumber("041234567");
        recipient.setUser(savedRecipientUser);
        recipientId = recipientProfileRepository.save(recipient).getId();
    }

    @Test
    void createDeliveryRequest_savesToDbAndSendsToQueue() {
        when(geocodingService.geocode(anyString()))
                .thenReturn(new Coordinates(-37.8136, 144.9631));
        when(pricingService.calculateDeliveryFee(any(), any(), any()))
                .thenReturn(new BigDecimal("25.00"));

        DeliveryRequestDto dto = new DeliveryRequestDto(
                senderId, recipientId,
                new AddressDto("123", "Main St", "Springfield", "VIC", "3000"),
                new AddressDto("456", "Oak Ave", "Shelbyville", "VIC", "3001"),
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
        assertThat(saved.getDeliveryFee()).isEqualByComparingTo("25.00");
        assertThat(deliveryRepository.findById(saved.getId())).isPresent();

        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> verify(deliveryRequestQueueHandler).handle(captor.capture()));

        assertThat(captor.getValue().getId()).isEqualTo(saved.getId());
    }
}