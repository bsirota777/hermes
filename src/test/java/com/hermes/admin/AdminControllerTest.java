package com.hermes.admin;

import com.hermes.delivery.Delivery;
import com.hermes.delivery.DeliveryRepository;
import com.hermes.delivery.DeliveryStatus;
import com.hermes.user.DriverProfile;
import com.hermes.user.RecipientProfile;
import com.hermes.user.SenderProfile;
import com.hermes.parcel.Parcel;
import com.hermes.parcel.ParcelRepository;
import com.hermes.user.Role;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ParcelRepository parcelRepository;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(userRepository, deliveryRepository, parcelRepository);
    }

    // ---------- getUsers ----------

    @Test
    void getUsers_appliesDefaultIdSort_whenPageableUnsorted() {
        Pageable unsorted = PageRequest.of(0, 20);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("Alice");
        when(user.getEmail()).thenReturn("alice@example.com");
        when(user.getRole()).thenReturn(Role.USER);
        when(user.isBanned()).thenReturn(false);
        when(user.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));

        Page<User> page = new PageImpl<>(List.of(user), unsorted, 1);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(captor.capture())).thenReturn(page);
        when(deliveryRepository.countBySender_User_Id(1L)).thenReturn(3L);
        when(deliveryRepository.countByRecipient_User_Id(1L)).thenReturn(2L);

        Page<AdminUserDto> result = controller.getUsers(unsorted);

        assertThat(captor.getValue().getSort().isSorted()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();

        assertThat(result.getContent()).hasSize(1);
        AdminUserDto dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Alice");
        assertThat(dto.email()).isEqualTo("alice@example.com");
        assertThat(dto.role()).isEqualTo("USER");
        assertThat(dto.banned()).isFalse();
        assertThat(dto.sentCount()).isEqualTo(3L);
        assertThat(dto.receivedCount()).isEqualTo(2L);
    }

    @Test
    void getUsers_keepsProvidedSort_whenAlreadySorted() {
        Pageable sorted = PageRequest.of(0, 20, Sort.by("name"));

        Page<User> emptyPage = new PageImpl<>(List.of(), sorted, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(captor.capture())).thenReturn(emptyPage);

        controller.getUsers(sorted);

        assertThat(captor.getValue()).isEqualTo(sorted);
    }

    // ---------- getDeliveries ----------

    @Test
    void getDeliveries_mapsAssignedDeliveryToDto() {
        Pageable unsorted = PageRequest.of(0, 20);

        User senderUser = mock(User.class);
        when(senderUser.getName()).thenReturn("Sender Sam");
        SenderProfile sender = mock(SenderProfile.class);
        when(sender.getUser()).thenReturn(senderUser);
        when(sender.getPhoneNumber()).thenReturn("0400111222");

        User recipientUser = mock(User.class);
        when(recipientUser.getName()).thenReturn("Recipient Rae");
        RecipientProfile recipient = mock(RecipientProfile.class);
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipient.getPhoneNumber()).thenReturn("0400333444");

        User driverUser = mock(User.class);
        when(driverUser.getName()).thenReturn("Driver Dan");
        DriverProfile driver = mock(DriverProfile.class);
        when(driver.getUser()).thenReturn(driverUser);
        when(driver.getLicenceNumber()).thenReturn("LIC123");

        Delivery delivery = mock(Delivery.class);
        when(delivery.getId()).thenReturn(100L);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.ASSIGNED);
        when(delivery.getSender()).thenReturn(sender);
        when(delivery.getRecipient()).thenReturn(recipient);
        when(delivery.getDriver()).thenReturn(driver);
        when(delivery.getPickUpAddress()).thenReturn("1 Pickup St");
        when(delivery.getDropOffAddress()).thenReturn("2 Dropoff Ave");
        when(delivery.getDeliveryFee()).thenReturn(new BigDecimal("15.50"));
        when(delivery.getParcels()).thenReturn(List.of(mock(Parcel.class), mock(Parcel.class)));
        when(delivery.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 12, 0));

        Page<Delivery> page = new PageImpl<>(List.of(delivery), unsorted, 1);
        when(deliveryRepository.findAllWithDetails(any(Pageable.class))).thenReturn(page);

        Page<AdminDeliveryDto> result = controller.getDeliveries(unsorted);

        assertThat(result.getContent()).hasSize(1);
        AdminDeliveryDto dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.status()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(dto.senderName()).isEqualTo("Sender Sam");
        assertThat(dto.senderPhone()).isEqualTo("0400111222");
        assertThat(dto.recipientName()).isEqualTo("Recipient Rae");
        assertThat(dto.recipientPhone()).isEqualTo("0400333444");
        assertThat(dto.driverName()).isEqualTo("Driver Dan");
        assertThat(dto.driverVerified()).isTrue();
        assertThat(dto.pickUpAddress()).isEqualTo("1 Pickup St");
        assertThat(dto.dropOffAddress()).isEqualTo("2 Dropoff Ave");
        assertThat(dto.deliveryFee()).isEqualByComparingTo("15.50");
        assertThat(dto.parcelCount()).isEqualTo(2);
    }

    @Test
    void getDeliveries_withNoDriverAssigned_returnsNullDriverFields() {
        Pageable unsorted = PageRequest.of(0, 20);

        User senderUser = mock(User.class);
        when(senderUser.getName()).thenReturn("Sender Sam");
        SenderProfile sender = mock(SenderProfile.class);
        when(sender.getUser()).thenReturn(senderUser);
        when(sender.getPhoneNumber()).thenReturn("0400111222");

        User recipientUser = mock(User.class);
        when(recipientUser.getName()).thenReturn("Recipient Rae");
        RecipientProfile recipient = mock(RecipientProfile.class);
        when(recipient.getUser()).thenReturn(recipientUser);
        when(recipient.getPhoneNumber()).thenReturn("0400333444");

        Delivery delivery = mock(Delivery.class);
        when(delivery.getId()).thenReturn(101L);
        when(delivery.getStatus()).thenReturn(DeliveryStatus.CREATED);
        when(delivery.getSender()).thenReturn(sender);
        when(delivery.getRecipient()).thenReturn(recipient);
        when(delivery.getDriver()).thenReturn(null);
        when(delivery.getPickUpAddress()).thenReturn("1 Pickup St");
        when(delivery.getDropOffAddress()).thenReturn("2 Dropoff Ave");
        when(delivery.getDeliveryFee()).thenReturn(new BigDecimal("10.00"));
        when(delivery.getParcels()).thenReturn(List.of());
        when(delivery.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 1, 12, 0));

        Page<Delivery> page = new PageImpl<>(List.of(delivery), unsorted, 1);
        when(deliveryRepository.findAllWithDetails(any(Pageable.class))).thenReturn(page);

        Page<AdminDeliveryDto> result = controller.getDeliveries(unsorted);

        AdminDeliveryDto dto = result.getContent().get(0);
        assertThat(dto.driverName()).isNull();
        assertThat(dto.driverVerified()).isFalse();
        assertThat(dto.parcelCount()).isZero();
    }

    // ---------- getParcelsForDelivery ----------

    @Test
    void getParcelsForDelivery_returnsMappedParcels() {
        Parcel parcel = mock(Parcel.class);
        when(parcel.getId()).thenReturn(500L);
        when(parcel.getDescription()).thenReturn("Fragile vase");
        when(parcel.getWeightKg()).thenReturn(new BigDecimal("2.50"));
        when(parcel.getDeclaredValue()).thenReturn(new BigDecimal("100.00"));
        when(parcel.isInsured()).thenReturn(true);
        when(parcel.getInsuredValue()).thenReturn(new BigDecimal("100.00"));

        when(parcelRepository.findByDeliveryId(100L)).thenReturn(List.of(parcel));

        ResponseEntity<List<AdminParcelDto>> response = controller.getParcelsForDelivery(100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        AdminParcelDto dto = response.getBody().get(0);
        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.description()).isEqualTo("Fragile vase");
        assertThat(dto.weightKg()).isEqualByComparingTo("2.50");
        assertThat(dto.declaredValue()).isEqualByComparingTo("100.00");
        assertThat(dto.insured()).isTrue();
        assertThat(dto.insuredValue()).isEqualByComparingTo("100.00");
    }

    // ---------- setBanned ----------

    @Test
    void setBanned_bansAnotherUser_succeeds() {
        User target = mock(User.class);
        when(target.getId()).thenReturn(2L);
        when(target.getEmail()).thenReturn("driver@example.com");
        when(target.getName()).thenReturn("Driver Dan");
        when(target.getRole()).thenReturn(Role.USER);
        when(target.isBanned()).thenReturn(true);
        when(target.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));

        UserDetails currentUser = mock(UserDetails.class);
        when(currentUser.getUsername()).thenReturn("admin@example.com");

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);
        when(deliveryRepository.countBySender_User_Id(2L)).thenReturn(0L);
        when(deliveryRepository.countByRecipient_User_Id(2L)).thenReturn(0L);

        ResponseEntity<AdminUserDto> response =
                controller.setBanned(2L, new BanRequest(true), currentUser);

        verify(target).setBanned(true);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().banned()).isTrue();
    }

    @Test
    void setBanned_userNotFound_throws404() {
        UserDetails currentUser = mock(UserDetails.class);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.setBanned(99L, new BanRequest(true), currentUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(userRepository, never()).save(any());
    }

    @Test
    void setBanned_adminBansSelf_throws400() {
        User self = mock(User.class);
        when(self.getEmail()).thenReturn("admin@example.com");

        UserDetails currentUser = mock(UserDetails.class);
        when(currentUser.getUsername()).thenReturn("admin@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> controller.setBanned(1L, new BanRequest(true), currentUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
    }

    @Test
    void setBanned_adminUnbansSelf_isAllowed() {
        User self = mock(User.class);
        when(self.getId()).thenReturn(1L);
        when(self.getName()).thenReturn("Admin User");
        when(self.getRole()).thenReturn(Role.ADMIN);
        when(self.isBanned()).thenReturn(false);
        when(self.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));

        UserDetails currentUser = mock(UserDetails.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(self));
        when(userRepository.save(self)).thenReturn(self);
        when(deliveryRepository.countBySender_User_Id(1L)).thenReturn(0L);
        when(deliveryRepository.countByRecipient_User_Id(1L)).thenReturn(0L);

        // request.banned() == false, so the self-ban check's email comparison never runs
        ResponseEntity<AdminUserDto> response =
                controller.setBanned(1L, new BanRequest(false), currentUser);

        verify(self).setBanned(false);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}