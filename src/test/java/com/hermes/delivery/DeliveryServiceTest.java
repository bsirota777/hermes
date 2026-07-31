package com.hermes.delivery;

import com.hermes.TestcontainersConfig;
import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.delivery.dto.ParcelDto;
import com.hermes.delivery.exception.InvalidDeliveryException;
import com.hermes.pricing.PricingService;
import com.hermes.geocoding.Coordinates;
import com.hermes.geocoding.GeocodingService;
import com.hermes.user.*;
import com.hermes.user.exception.RecipientProfileNotFoundException;
import com.hermes.user.exception.SenderProfileNotFoundException;
import com.hermes.wallet.WalletService;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Import(TestcontainersConfig.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private SenderProfileRepository senderProfileRepository;

    @Mock
    private RecipientProfileRepository recipientProfileRepository;

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private ProducerTemplate producerTemplate;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private PricingService pricingService;

    private DeliveryService deliveryService;

    private static final List<ParcelDto> DEFAULT_PARCELS = List.of(new ParcelDto(
            "Test parcel",
            new BigDecimal("10.00"),
            new BigDecimal("10.00"),
            new BigDecimal("10.00"),
            new BigDecimal("2.00"),
            new BigDecimal("50.00"),
            false,
            null
    ));

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(deliveryRepository, producerTemplate,
                senderProfileRepository, recipientProfileRepository, parcelRepository, walletService,
                geocodingService, pricingService);
    }

    private User buildUser(Long id, String email) {
        User user = new User("Test User", email, "secret");
        user.setId(id);
        return user;
    }

    private SenderProfile buildSender(Long id, Long userId, String email) {
        SenderProfile sender = new SenderProfile();
        sender.setId(id);
        sender.setUser(buildUser(userId, email));
        return sender;
    }

    private RecipientProfile buildRecipient(Long id, Long userId, String email) {
        RecipientProfile recipient = new RecipientProfile();
        recipient.setId(id);
        recipient.setUser(buildUser(userId, email));
        return recipient;
    }

    private void stubGeocodingAndPricing() {
        when(geocodingService.geocode(anyString())).thenReturn(new Coordinates(-37.8136, 144.9631));
        when(pricingService.calculateDeliveryFee(any(), any(), any())).thenReturn(new BigDecimal("25.00"));
    }

    @Test
    void createDeliveryRequest_savesDeliveryAndPublishesToQueue_whenSenderAndRecipientDifferent() {
        SenderProfile sender = buildSender(1L, 1L, "sender@example.com");
        RecipientProfile recipient = buildRecipient(2L, 2L, "recipient@example.com");

        DeliveryRequestDto request = new DeliveryRequestDto(
                sender.getId(), recipient.getId(), "123 Pickup St", "456 Dropoff Ave", DEFAULT_PARCELS);

        when(senderProfileRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(recipientProfileRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubGeocodingAndPricing();

        Delivery result = deliveryService.createDeliveryRequest(request);

        assertThat(result.getSender()).isEqualTo(sender);
        assertThat(result.getRecipient()).isEqualTo(recipient);
        assertThat(result.getPickUpAddress()).isEqualTo("123 Pickup St");
        assertThat(result.getDropOffAddress()).isEqualTo("456 Dropoff Ave");
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CREATED);
        assertThat(result.getDeliveryFee()).isEqualByComparingTo("25.00");

        verify(deliveryRepository).save(any(Delivery.class));
        verify(parcelRepository).save(any(Parcel.class));
        verify(producerTemplate).sendBody(eq("seda:delivery-requests"), any(Delivery.class));
    }

    @Test
    void createDeliveryRequest_throws_whenSenderAndRecipientAreSameUser() {
        SenderProfile sender = buildSender(1L, 1L, "same@example.com");
        RecipientProfile recipient = buildRecipient(2L, 1L, "same@example.com");

        DeliveryRequestDto request = new DeliveryRequestDto(
                sender.getId(), recipient.getId(), "123 Pickup St", "456 Dropoff Ave", DEFAULT_PARCELS);

        when(senderProfileRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(recipientProfileRepository.findById(recipient.getId())).thenReturn(Optional.of(recipient));

        assertThatThrownBy(() -> deliveryService.createDeliveryRequest(request))
                .isInstanceOf(InvalidDeliveryException.class)
                .hasMessageContaining("Sender and recipient cannot be the same user");

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(producerTemplate);
        verifyNoInteractions(parcelRepository);
        verifyNoInteractions(geocodingService);
        verifyNoInteractions(pricingService);
    }

    @Test
    void createDeliveryRequest_throwsSenderProfileNotFoundException_whenSenderMissing() {
        DeliveryRequestDto request = new DeliveryRequestDto(
                99L, 2L, "123 Pickup St", "456 Dropoff Ave", DEFAULT_PARCELS);

        when(senderProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.createDeliveryRequest(request))
                .isInstanceOf(SenderProfileNotFoundException.class);

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(producerTemplate);
        verifyNoInteractions(geocodingService);
    }

    @Test
    void createDeliveryRequest_throwsRecipientProfileNotFoundException_whenRecipientMissing() {
        SenderProfile sender = buildSender(1L, 1L, "sender@example.com");
        DeliveryRequestDto request = new DeliveryRequestDto(
                sender.getId(), 99L, "123 Pickup St", "456 Dropoff Ave", DEFAULT_PARCELS);

        when(senderProfileRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(recipientProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.createDeliveryRequest(request))
                .isInstanceOf(RecipientProfileNotFoundException.class);

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(producerTemplate);
        verifyNoInteractions(geocodingService);
    }

    @Test
    void getInTransitDeliveries_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable))
                .thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getInTransitDeliveries(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
    }
}