package com.hermes.delivery;

import com.hermes.common.address.AddressDto;
import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.FindOrCreateProfileRequest;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.common.user.UserSummary;
import com.hermes.common.wallet.CreditWalletRequest;
import com.hermes.common.wallet.WalletTransactionType;
import com.hermes.delivery.client.ProfileServiceClient;
import com.hermes.delivery.client.UserServiceClient;
import com.hermes.delivery.client.WalletServiceClient;
import com.hermes.delivery.dto.CreateDeliveryRequest;
import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.delivery.exception.*;
import com.hermes.delivery.geocoding.Coordinates;
import com.hermes.delivery.geocoding.GeocodingService;
import com.hermes.delivery.parcel.Parcel;
import com.hermes.delivery.parcel.ParcelRepository;
import com.hermes.delivery.parcel.dto.ParcelDto;
import com.hermes.delivery.pricing.PricingService;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private ProducerTemplate producerTemplate;
    @Mock private ParcelRepository parcelRepository;
    @Mock private GeocodingService geocodingService;
    @Mock private PricingService pricingService;
    @Mock private ProfileServiceClient profileServiceClient;
    @Mock private WalletServiceClient walletServiceClient;
    @Mock private UserServiceClient userServiceClient;

    private DeliveryService deliveryService;

    private static final List<ParcelDto> DEFAULT_PARCELS = List.of(new ParcelDto(
            "Test parcel",
            new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"),
            new BigDecimal("2.00"), new BigDecimal("50.00"), false, null
    ));

    private static final AddressDto PICKUP_ADDRESS =
            new AddressDto("123", "Pickup St", "Springfield", "VIC", "3000");
    private static final AddressDto DROPOFF_ADDRESS =
            new AddressDto("456", "Dropoff Ave", "Shelbyville", "VIC", "3001");

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(deliveryRepository, producerTemplate, parcelRepository,
                geocodingService, pricingService, profileServiceClient, walletServiceClient, userServiceClient);
    }

    private void stubGeocodingAndPricing() {
        when(geocodingService.geocode(anyString())).thenReturn(new Coordinates(-37.8136, 144.9631));
        when(pricingService.calculateDeliveryFee(any(), any(), any())).thenReturn(new BigDecimal("25.00"));
    }

    private HttpClientErrorException notFound() {
        return HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null);
    }

    // BigDecimal arithmetic (e.g. fee.multiply(rate)) can produce a different scale than a
    // hand-typed literal even when numerically equal (20.00 * 0.80 = 16.0000, not 16.00) -
    // CreditWalletRequest is a record, so its generated equals() compares BigDecimal scale too,
    // making direct equality brittle here. Compare by value instead.
    private CreditWalletRequest creditRequestMatching(Long userId, BigDecimal amount,
                                                      WalletTransactionType type, Long deliveryId) {
        return argThat(req -> req != null
                && req.userId().equals(userId)
                && req.amount().compareTo(amount) == 0
                && req.type() == type
                && java.util.Objects.equals(req.deliveryId(), deliveryId));
    }

    // ---------- createDeliveryRequest ----------

    @Test
    void createDeliveryRequest_savesDeliveryAndPublishesToQueue_whenSenderAndRecipientDifferent() {
        ProfileSummary sender = new ProfileSummary(1L, 10L, "Sender Name");
        ProfileSummary recipient = new ProfileSummary(2L, 20L, "Recipient Name");

        DeliveryRequestDto request = new DeliveryRequestDto(
                sender.profileId(), recipient.profileId(), PICKUP_ADDRESS, DROPOFF_ADDRESS, DEFAULT_PARCELS);

        when(profileServiceClient.getSenderProfile(1L)).thenReturn(sender);
        when(profileServiceClient.getRecipientProfile(2L)).thenReturn(recipient);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        stubGeocodingAndPricing();

        Delivery result = deliveryService.createDeliveryRequest(request);

        assertThat(result.getSenderId()).isEqualTo(1L);
        assertThat(result.getRecipientId()).isEqualTo(2L);
        assertThat(result.getPickUpAddress().getStreetNumber()).isEqualTo("123");
        assertThat(result.getDropOffAddress().getStreetNumber()).isEqualTo("456");
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.CREATED);
        assertThat(result.getDeliveryFee()).isEqualByComparingTo("25.00");
        assertThat(result.getQrCodeToken()).isNotBlank();

        verify(deliveryRepository).save(any(Delivery.class));
        verify(parcelRepository).save(any(Parcel.class));
        verify(producerTemplate).sendBody(eq("seda:delivery-requests"), any(Delivery.class));
    }

    @Test
    void createDeliveryRequest_throws_whenSenderAndRecipientAreSameUser() {
        ProfileSummary sender = new ProfileSummary(1L, 10L, "Same Name");
        ProfileSummary recipient = new ProfileSummary(2L, 10L, "Same Name");

        DeliveryRequestDto request = new DeliveryRequestDto(
                sender.profileId(), recipient.profileId(), PICKUP_ADDRESS, DROPOFF_ADDRESS, DEFAULT_PARCELS);

        when(profileServiceClient.getSenderProfile(1L)).thenReturn(sender);
        when(profileServiceClient.getRecipientProfile(2L)).thenReturn(recipient);

        assertThatThrownBy(() -> deliveryService.createDeliveryRequest(request))
                .isInstanceOf(InvalidDeliveryException.class)
                .hasMessageContaining("Sender and recipient cannot be the same user");

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(producerTemplate, parcelRepository, geocodingService, pricingService);
    }

    @Test
    void createDeliveryRequest_throwsSenderProfileNotFoundException_whenSenderMissing() {
        DeliveryRequestDto request = new DeliveryRequestDto(
                99L, 2L, PICKUP_ADDRESS, DROPOFF_ADDRESS, DEFAULT_PARCELS);

        when(profileServiceClient.getSenderProfile(99L)).thenThrow(notFound());

        assertThatThrownBy(() -> deliveryService.createDeliveryRequest(request))
                .isInstanceOf(SenderProfileNotFoundException.class);

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(producerTemplate, geocodingService);
    }

    @Test
    void createDeliveryRequest_throwsRecipientProfileNotFoundException_whenRecipientMissing() {
        ProfileSummary sender = new ProfileSummary(1L, 10L, "Sender Name");
        DeliveryRequestDto request = new DeliveryRequestDto(
                sender.profileId(), 99L, PICKUP_ADDRESS, DROPOFF_ADDRESS, DEFAULT_PARCELS);

        when(profileServiceClient.getSenderProfile(1L)).thenReturn(sender);
        when(profileServiceClient.getRecipientProfile(99L)).thenThrow(notFound());

        assertThatThrownBy(() -> deliveryService.createDeliveryRequest(request))
                .isInstanceOf(RecipientProfileNotFoundException.class);

        verify(deliveryRepository, never()).save(any());
        verifyNoInteractions(producerTemplate, geocodingService);
    }

    // ---------- createDelivery ----------

    @Test
    void createDelivery_findsOrCreatesProfiles_thenDelegatesToCreateDeliveryRequest() {
        Long senderUserId = 1L;
        UserSummary recipientUser = new UserSummary(2L, "Recipient Name", "recipient@example.com");
        ProfileSummary senderProfile = new ProfileSummary(10L, senderUserId, "Sender Name");
        ProfileSummary recipientProfile = new ProfileSummary(20L, recipientUser.id(), recipientUser.name());

        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "recipient@example.com", PICKUP_ADDRESS, DROPOFF_ADDRESS,
                "0400111222", "0400333444", DEFAULT_PARCELS);

        when(userServiceClient.findUserByEmail("recipient@example.com")).thenReturn(Optional.of(recipientUser));
        when(profileServiceClient.findOrCreateSenderProfile(any(FindOrCreateProfileRequest.class)))
                .thenReturn(senderProfile);
        when(profileServiceClient.findOrCreateRecipientProfile(any(FindOrCreateProfileRequest.class)))
                .thenReturn(recipientProfile);
        when(profileServiceClient.getSenderProfile(10L)).thenReturn(senderProfile);
        when(profileServiceClient.getRecipientProfile(20L)).thenReturn(recipientProfile);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        stubGeocodingAndPricing();

        Delivery result = deliveryService.createDelivery(senderUserId, request);

        assertThat(result.getSenderId()).isEqualTo(10L);
        assertThat(result.getRecipientId()).isEqualTo(20L);

        verify(profileServiceClient).findOrCreateSenderProfile(
                new FindOrCreateProfileRequest(senderUserId, PICKUP_ADDRESS, "0400111222"));
        verify(profileServiceClient).findOrCreateRecipientProfile(
                new FindOrCreateProfileRequest(recipientUser.id(), DROPOFF_ADDRESS, "0400333444"));
        verify(producerTemplate).sendBody(eq("seda:delivery-requests"), any(Delivery.class));
    }

    @Test
    void createDelivery_throwsUserNotFoundException_whenRecipientEmailUnknown() {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "nobody@example.com", PICKUP_ADDRESS, DROPOFF_ADDRESS,
                "0400111222", "0400333444", DEFAULT_PARCELS);

        when(userServiceClient.findUserByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.createDelivery(1L, request))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(profileServiceClient, deliveryRepository, producerTemplate);
    }

    // ---------- getInTransitDeliveries ----------

    @Test
    void getInTransitDeliveries_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable)).thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getInTransitDeliveries(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
    }

    // ---------- updateStatus / refunds / payouts ----------

    @Test
    void updateStatus_refundsFullFee_whenCancelledWithNoDriverAssigned() {
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setSenderId(1L);
        delivery.setStatus(DeliveryStatus.CREATED);
        delivery.setDeliveryFee(new BigDecimal("25.00"));

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileServiceClient.getSenderProfile(1L)).thenReturn(new ProfileSummary(1L, 100L, "Sender"));

        deliveryService.updateStatus(1L, DeliveryStatus.CANCELLED);

        verify(walletServiceClient).credit(
                creditRequestMatching(100L, new BigDecimal("25.00"), WalletTransactionType.REFUND, 1L));
    }

    @Test
    void updateStatus_refunds80Percent_whenCancelledWithDriverAssigned() {
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setSenderId(1L);
        delivery.setDriverId(5L);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setDeliveryFee(new BigDecimal("25.00"));

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileServiceClient.getSenderProfile(1L)).thenReturn(new ProfileSummary(1L, 100L, "Sender"));

        deliveryService.updateStatus(1L, DeliveryStatus.CANCELLED);

        verify(walletServiceClient).credit(
                creditRequestMatching(100L, new BigDecimal("20.00"), WalletTransactionType.REFUND, 1L));
    }

    // ---------- getSentDeliveries / getReceivedDeliveries ----------

    @Test
    void getSentDeliveries_returnsPage_whenSenderProfileExists() {
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(profileServiceClient.findSenderProfileByUserId(1L))
                .thenReturn(Optional.of(new ProfileSummary(10L, 1L, "Sender")));
        when(deliveryRepository.findBySenderId(10L, pageable)).thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getSentDeliveries(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getSentDeliveries_returnsEmptyPage_whenNoSenderProfile() {
        Pageable pageable = PageRequest.of(0, 20);
        when(profileServiceClient.findSenderProfileByUserId(1L)).thenReturn(Optional.empty());

        Page<Delivery> result = deliveryService.getSentDeliveries(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(deliveryRepository);
    }

    @Test
    void getReceivedDeliveries_returnsPage_whenRecipientProfileExists() {
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(profileServiceClient.findRecipientProfileByUserId(2L))
                .thenReturn(Optional.of(new ProfileSummary(20L, 2L, "Recipient")));
        when(deliveryRepository.findByRecipientId(20L, pageable)).thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getReceivedDeliveries(2L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getReceivedDeliveries_returnsEmptyPage_whenNoRecipientProfile() {
        Pageable pageable = PageRequest.of(0, 20);
        when(profileServiceClient.findRecipientProfileByUserId(2L)).thenReturn(Optional.empty());

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
        DriverProfileSummary driver = new DriverProfileSummary(50L, 300L, "Driver", -37.8136, 144.9631);

        Delivery near = new Delivery();
        near.setId(1L);
        near.setPickUpLatitude(-37.8140);
        near.setPickUpLongitude(144.9633);

        Delivery far = new Delivery();
        far.setId(2L);
        far.setPickUpLatitude(-33.8688); // Sydney
        far.setPickUpLongitude(151.2093);

        when(profileServiceClient.getDriverProfile(50L)).thenReturn(driver);
        when(deliveryRepository.findByDriverIdIsNull(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(far, near)));

        Page<Delivery> result = deliveryService.getQueueForDriver(50L, 0, 10);

        assertThat(result.getContent()).containsExactly(near, far);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getQueueForDriver_appliesPaginationAfterSorting() {
        DriverProfileSummary driver = new DriverProfileSummary(50L, 300L, "Driver", -37.8136, 144.9631);

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

        when(profileServiceClient.getDriverProfile(50L)).thenReturn(driver);
        when(deliveryRepository.findByDriverIdIsNull(Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(farthest, closest, middle)));

        Page<Delivery> result = deliveryService.getQueueForDriver(50L, 1, 1);

        assertThat(result.getContent()).containsExactly(middle);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    // ---------- reserve ----------

    @Test
    void reserve_assignsDriverAndSetsStatusAssigned() {
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setStatus(DeliveryStatus.CREATED);

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        Delivery result = deliveryService.reserve(1L, 50L);

        assertThat(result.getDriverId()).isEqualTo(50L);
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
    }

    @Test
    void reserve_throwsDeliveryAlreadyAssigned_whenDriverAlreadySet() {
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setDriverId(99L);
        delivery.setStatus(DeliveryStatus.ASSIGNED);

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThrows(DeliveryAlreadyAssignedException.class, () -> deliveryService.reserve(1L, 50L));

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void reserve_throwsInvalidStatusTransition_whenNotAllowedFromCurrentStatus() {
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setStatus(DeliveryStatus.DELIVERED);

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThrows(InvalidStatusTransitionException.class, () -> deliveryService.reserve(1L, 50L));
    }

    // ---------- completeDelivery ----------

    @Test
    void completeDelivery_marksDelivered_whenTokenMatches() {
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setQrCodeToken("abc-123-token");
        delivery.setDriverId(50L);
        delivery.setDeliveryFee(new BigDecimal("20.00"));
        delivery.setDriverCommissionRate(new BigDecimal("0.80"));

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileServiceClient.getDriverProfile(50L))
                .thenReturn(new DriverProfileSummary(50L, 500L, "Driver", -37.8, 144.9));

        Delivery result = deliveryService.completeDelivery(1L, "abc-123-token");

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(walletServiceClient).credit(
                creditRequestMatching(500L, new BigDecimal("16.00"), WalletTransactionType.EARNING, 1L));
    }

    @Test
    void completeDelivery_throwsInvalidQrCodeException_whenTokenMismatch() {
        Delivery delivery = new Delivery();
        delivery.setId(1L);
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setQrCodeToken("abc-123-token");

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThrows(InvalidQrCodeException.class,
                () -> deliveryService.completeDelivery(1L, "wrong-token"));

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void completeDelivery_throwsDeliveryNotFoundException_whenDeliveryMissing() {
        when(deliveryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DeliveryNotFoundException.class,
                () -> deliveryService.completeDelivery(99L, "any-token"));
    }
}
