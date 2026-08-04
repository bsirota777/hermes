package com.hermes.delivery;

import com.hermes.TestcontainersConfig;
import com.hermes.delivery.dto.CreateDeliveryRequest;
import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.parcel.dto.ParcelDto;
import com.hermes.delivery.exception.InvalidDeliveryException;
import com.hermes.parcel.Parcel;
import com.hermes.parcel.ParcelRepository;
import com.hermes.pricing.PricingService;
import com.hermes.geocoding.Coordinates;
import com.hermes.geocoding.GeocodingService;
import com.hermes.user.*;
import com.hermes.user.exception.RecipientProfileNotFoundException;
import com.hermes.user.exception.SenderProfileNotFoundException;
import com.hermes.user.exception.UserNotFoundException;
import com.hermes.wallet.WalletService;
import com.hermes.wallet.WalletTransactionType;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private UserRepository userRepository;

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
                geocodingService, pricingService, userRepository);
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

    @Test
    void updateStatus_refundsFullFee_whenCancelledWithNoDriverAssigned() {
        SenderProfile sender = buildSender(1L, 1L, "sender@example.com");
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setSender(sender);
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setDeliveryFee(new BigDecimal("25.00"));

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        deliveryService.updateStatus(1L, DeliveryStatus.CANCELLED);

        verify(walletService).credit(
                eq(1L),
                argThat(amount -> amount.compareTo(new BigDecimal("25.00")) == 0),
                eq(WalletTransactionType.REFUND),
                eq(delivery));
    }

    @Test
    void updateStatus_refunds80Percent_whenCancelledWithDriverAssigned() {
        SenderProfile sender = buildSender(1L, 1L, "sender@example.com");
        DriverProfile driver = new DriverProfile();
        driver.setId(1L);
        driver.setUser(buildUser(2L, "driver@example.com"));

        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setSender(sender);
        delivery.setDriver(driver);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setDeliveryFee(new BigDecimal("25.00"));

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        deliveryService.updateStatus(1L, DeliveryStatus.CANCELLED);

        verify(walletService).credit(
                eq(1L),
                argThat(amount -> amount.compareTo(new BigDecimal("20.00")) == 0),
                eq(WalletTransactionType.REFUND),
                eq(delivery));
    }

    // ---------- createDelivery ----------

    @Test
    void createDelivery_createsMissingSenderAndRecipientProfiles_thenDelegatesToCreateDeliveryRequest() {
        User senderUser = buildUser(1L, "sender@example.com");
        User recipientUser = buildUser(2L, "recipient@example.com");

        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "recipient@example.com", "123 Pickup St", "456 Dropoff Ave",
                "0400111222", "0400333444", DEFAULT_PARCELS);

        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipientUser));

        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        SenderProfile savedSender = buildSender(10L, 1L, "sender@example.com");
        when(senderProfileRepository.save(any(SenderProfile.class))).thenReturn(savedSender);
        when(senderProfileRepository.findById(10L)).thenReturn(Optional.of(savedSender));

        when(recipientProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        RecipientProfile savedRecipient = buildRecipient(20L, 2L, "recipient@example.com");
        when(recipientProfileRepository.save(any(RecipientProfile.class))).thenReturn(savedRecipient);
        when(recipientProfileRepository.findById(20L)).thenReturn(Optional.of(savedRecipient));

        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        stubGeocodingAndPricing();

        Delivery result = deliveryService.createDelivery(senderUser, request);

        assertThat(result.getSender()).isEqualTo(savedSender);
        assertThat(result.getRecipient()).isEqualTo(savedRecipient);
        assertThat(result.getPickUpAddress()).isEqualTo("123 Pickup St");
        assertThat(result.getDropOffAddress()).isEqualTo("456 Dropoff Ave");

        ArgumentCaptor<SenderProfile> senderCaptor = ArgumentCaptor.forClass(SenderProfile.class);
        verify(senderProfileRepository).save(senderCaptor.capture());
        assertThat(senderCaptor.getValue().getAddress()).isEqualTo("123 Pickup St");
        assertThat(senderCaptor.getValue().getPhoneNumber()).isEqualTo("0400111222");
        assertThat(senderCaptor.getValue().getUser()).isEqualTo(senderUser);

        ArgumentCaptor<RecipientProfile> recipientCaptor = ArgumentCaptor.forClass(RecipientProfile.class);
        verify(recipientProfileRepository).save(recipientCaptor.capture());
        assertThat(recipientCaptor.getValue().getAddress()).isEqualTo("456 Dropoff Ave");
        assertThat(recipientCaptor.getValue().getPhoneNumber()).isEqualTo("0400333444");
        assertThat(recipientCaptor.getValue().getUser()).isEqualTo(recipientUser);

        verify(producerTemplate).sendBody(eq("seda:delivery-requests"), any(Delivery.class));
    }

    @Test
    void createDelivery_reusesExistingProfiles_whenBothAlreadyExist() {
        User senderUser = buildUser(1L, "sender@example.com");
        User recipientUser = buildUser(2L, "recipient@example.com");
        SenderProfile existingSender = buildSender(10L, 1L, "sender@example.com");
        RecipientProfile existingRecipient = buildRecipient(20L, 2L, "recipient@example.com");

        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "recipient@example.com", "123 Pickup St", "456 Dropoff Ave",
                "0400111222", "0400333444", DEFAULT_PARCELS);

        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipientUser));
        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.of(existingSender));
        when(recipientProfileRepository.findByUserId(2L)).thenReturn(Optional.of(existingRecipient));
        when(senderProfileRepository.findById(10L)).thenReturn(Optional.of(existingSender));
        when(recipientProfileRepository.findById(20L)).thenReturn(Optional.of(existingRecipient));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        stubGeocodingAndPricing();

        Delivery result = deliveryService.createDelivery(senderUser, request);

        assertThat(result.getSender()).isEqualTo(existingSender);
        assertThat(result.getRecipient()).isEqualTo(existingRecipient);
        verify(senderProfileRepository, never()).save(any());
        verify(recipientProfileRepository, never()).save(any());
    }

    @Test
    void createDelivery_throwsUserNotFoundException_whenRecipientEmailUnknown() {
        User senderUser = buildUser(1L, "sender@example.com");
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "nobody@example.com", "123 Pickup St", "456 Dropoff Ave",
                "0400111222", "0400333444", DEFAULT_PARCELS);

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.createDelivery(senderUser, request))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(senderProfileRepository);
        verifyNoInteractions(recipientProfileRepository);
        verifyNoInteractions(deliveryRepository);
        verifyNoInteractions(producerTemplate);
    }

    @Test
    void createDelivery_throwsInvalidDeliveryException_whenRecipientEmailIsSendersOwnEmail() {
        User senderUser = buildUser(1L, "same@example.com");
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "same@example.com", "123 Pickup St", "456 Dropoff Ave",
                "0400111222", "0400333444", DEFAULT_PARCELS);

        when(userRepository.findByEmail("same@example.com")).thenReturn(Optional.of(senderUser));
        SenderProfile senderProfile = buildSender(10L, 1L, "same@example.com");
        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.of(senderProfile));
        RecipientProfile recipientProfile = buildRecipient(20L, 1L, "same@example.com");
        when(recipientProfileRepository.findByUserId(1L)).thenReturn(Optional.of(recipientProfile));
        when(senderProfileRepository.findById(10L)).thenReturn(Optional.of(senderProfile));
        when(recipientProfileRepository.findById(20L)).thenReturn(Optional.of(recipientProfile));

        assertThatThrownBy(() -> deliveryService.createDelivery(senderUser, request))
                .isInstanceOf(InvalidDeliveryException.class);

        verify(deliveryRepository, never()).save(any());
    }

    // ---------- getSentDeliveries ----------

    @Test
    void getSentDeliveries_returnsPage_whenSenderProfileExists() {
        SenderProfile sender = buildSender(10L, 1L, "sender@example.com");
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(deliveryRepository.findBySenderId(10L, pageable)).thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getSentDeliveries(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSentDeliveries_returnsEmptyPage_whenNoSenderProfile() {
        Pageable pageable = PageRequest.of(0, 20);
        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Page<Delivery> result = deliveryService.getSentDeliveries(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(deliveryRepository);
    }

// ---------- getReceivedDeliveries ----------

    @Test
    void getReceivedDeliveries_returnsPage_whenRecipientProfileExists() {
        RecipientProfile recipient = buildRecipient(20L, 2L, "recipient@example.com");
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(recipientProfileRepository.findByUserId(2L)).thenReturn(Optional.of(recipient));
        when(deliveryRepository.findByRecipientId(20L, pageable)).thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getReceivedDeliveries(2L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getReceivedDeliveries_returnsEmptyPage_whenNoRecipientProfile() {
        Pageable pageable = PageRequest.of(0, 20);
        when(recipientProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());

        Page<Delivery> result = deliveryService.getReceivedDeliveries(2L, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(deliveryRepository);
    }

// ---------- getDrivenDeliveries ----------

    @Test
    void getDrivenDeliveries_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(deliveryRepository.findByDriverId(30L, pageable)).thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getDrivenDeliveries(30L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

// ---------- getQueueForDriver ----------

    @Test
    void getQueueForDriver_sortsUnassignedDeliveriesByDistanceFromDriver() {
        DriverProfile driver = new DriverProfile();
        driver.setAddress("Melbourne CBD");

        Delivery near = new Delivery();
        near.setId(1L);
        near.setPickUpLatitude(-37.8140);
        near.setPickUpLongitude(144.9633);

        Delivery far = new Delivery();
        far.setId(2L);
        far.setPickUpLatitude(-33.8688); // Sydney
        far.setPickUpLongitude(151.2093);

        when(geocodingService.geocode("Melbourne CBD"))
                .thenReturn(new Coordinates(-37.8136, 144.9631));
        when(deliveryRepository.findByDriverIsNull(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(far, near)));

        Page<Delivery> result = deliveryService.getQueueForDriver(driver, 0, 10);

        assertThat(result.getContent()).containsExactly(near, far);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(geocodingService, times(1)).geocode("Melbourne CBD");
    }

    @Test
    void getQueueForDriver_appliesPaginationAfterSorting() {
        DriverProfile driver = new DriverProfile();
        driver.setAddress("Melbourne CBD");

        Delivery closest = new Delivery();
        closest.setId(1L);
        closest.setPickUpLatitude(-37.8136);
        closest.setPickUpLongitude(144.9631);

        Delivery middle = new Delivery();
        middle.setId(2L);
        middle.setPickUpLatitude(-37.9000);
        middle.setPickUpLongitude(145.0000);

        Delivery farthest = new Delivery();
        farthest.setId(3L);
        farthest.setPickUpLatitude(-33.8688);
        farthest.setPickUpLongitude(151.2093);

        when(geocodingService.geocode("Melbourne CBD"))
                .thenReturn(new Coordinates(-37.8136, 144.9631));
        when(deliveryRepository.findByDriverIsNull(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(farthest, closest, middle)));

        Page<Delivery> result = deliveryService.getQueueForDriver(driver, 1, 1);

        assertThat(result.getContent()).containsExactly(middle);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }
}