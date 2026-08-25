package com.hermes.delivery;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.common.user.UserSummary;
import com.hermes.delivery.client.ProfileServiceClient;
import com.hermes.delivery.client.UserServiceClient;
import com.hermes.delivery.dto.CreateDeliveryRequest;
import com.hermes.delivery.dto.DeliveryDto;
import com.hermes.delivery.exception.DeliveryAlreadyAssignedException;
import com.hermes.delivery.exception.DeliveryNotFoundException;
import com.hermes.delivery.exception.InvalidQrCodeException;
import com.hermes.delivery.exception.InvalidStatusTransitionException;
import com.hermes.delivery.mapper.DeliveryMapper;
import com.hermes.delivery.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

// jwt.secret must decode (base64) to >=32 bytes for SecurityConfig's JwtValidator bean to
// construct - value here is a throwaway test-only secret, never used to sign real tokens.
@WebMvcTest(DeliveryController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=")
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryService deliveryService;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private ProfileServiceClient profileServiceClient;

    @MockitoBean
    private DeliveryMapper deliveryMapper;

    private static final UserSummary SENDER = new UserSummary(100L, "Alice Sender", "alice@example.com");
    private static final UserSummary RECIPIENT = new UserSummary(200L, "Bob Recipient", "bob@example.com");
    private static final UserSummary DRIVER = new UserSummary(300L, "Charlie Driver", "charlie@example.com");
    private static final DriverProfileSummary DRIVER_PROFILE =
            new DriverProfileSummary(50L, DRIVER.id(), DRIVER.name(), -37.8136, 144.9631);

    private Delivery buildDelivery(Long id, DeliveryStatus status, Long driverId) {
        Delivery delivery = new Delivery();
        delivery.setId(id);
        delivery.setStatus(status);
        delivery.setSenderId(1L);
        delivery.setRecipientId(2L);
        delivery.setDriverId(driverId);
        delivery.setCreatedAt(LocalDateTime.of(2026, 7, 26, 10, 0));
        return delivery;
    }

    private DeliveryDto dtoFor(Delivery delivery) {
        return new DeliveryDto(delivery.getId(), delivery.getStatus(), delivery.getCreatedAt(),
                SENDER.id(), SENDER.name(), RECIPIENT.id(), RECIPIENT.name(),
                delivery.getDriverId() == null ? null : DRIVER.id(),
                delivery.getDriverId() == null ? null : DRIVER.name());
    }

    // --- in-transit ---

    @Test
    void getInTransitDeliveries_returnsMappedDtos() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);
        Page<Delivery> page = new PageImpl<>(List.of(delivery), PageRequest.of(0, 20), 1);

        when(deliveryService.getInTransitDeliveries(any(Pageable.class))).thenReturn(page);
        when(deliveryMapper.toDto(delivery)).thenReturn(dtoFor(delivery));

        mockMvc.perform(get("/deliveries/in-transit").with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.content[0].senderId").value(100))
                .andExpect(jsonPath("$.content[0].senderName").value("Alice Sender"))
                .andExpect(jsonPath("$.content[0].driverId").value(300));
    }

    @Test
    void getInTransitDeliveries_returnsEmptyPage_whenNoneInTransit() throws Exception {
        Page<Delivery> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(deliveryService.getInTransitDeliveries(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/deliveries/in-transit").with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // --- assign ---

    @Test
    void assignDriver_returnsUpdatedDelivery_onSuccess() throws Exception {
        Delivery assigned = buildDelivery(1L, DeliveryStatus.ASSIGNED, 50L);

        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(profileServiceClient.findDriverProfileByUserId(300L)).thenReturn(Optional.of(DRIVER_PROFILE));
        when(deliveryService.reserve(1L, 50L)).thenReturn(assigned);
        when(deliveryMapper.toDto(assigned)).thenReturn(dtoFor(assigned));

        mockMvc.perform(post("/deliveries/1/assign").with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void assignDriver_returns404_whenDeliveryNotFound() throws Exception {
        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(profileServiceClient.findDriverProfileByUserId(300L)).thenReturn(Optional.of(DRIVER_PROFILE));
        when(deliveryService.reserve(99L, 50L)).thenThrow(new DeliveryNotFoundException(99L));

        mockMvc.perform(post("/deliveries/99/assign").with(user("charlie@example.com")))
                .andDo(print())
                .andExpect(status().isNotFound());
                //.andExpect(status().isNotFound());
    }

    @Test
    void assignDriver_returns409_whenAlreadyAssigned() throws Exception {
        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(profileServiceClient.findDriverProfileByUserId(300L)).thenReturn(Optional.of(DRIVER_PROFILE));
        when(deliveryService.reserve(1L, 50L)).thenThrow(new DeliveryAlreadyAssignedException(1L));

        mockMvc.perform(post("/deliveries/1/assign").with(user("charlie@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void assignDriver_returns409_whenInvalidStatusTransition() throws Exception {
        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(profileServiceClient.findDriverProfileByUserId(300L)).thenReturn(Optional.of(DRIVER_PROFILE));
        when(deliveryService.reserve(1L, 50L))
                .thenThrow(new InvalidStatusTransitionException(DeliveryStatus.IN_TRANSIT, DeliveryStatus.ASSIGNED));

        mockMvc.perform(post("/deliveries/1/assign").with(user("charlie@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void assignDriver_returns404_whenDriverProfileNotFound() throws Exception {
        UserSummary noProfileUser = new UserSummary(400L, "Dana NoProfile", "dana@example.com");
        when(userServiceClient.findUserByEmail("dana@example.com")).thenReturn(Optional.of(noProfileUser));
        when(profileServiceClient.findDriverProfileByUserId(400L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/deliveries/1/assign").with(user("dana@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignDriver_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/deliveries/1/assign"))
                .andExpect(status().isForbidden());
    }

    // --- start ---

    @Test
    void startTransit_shouldReturnUpdatedDelivery_whenCallerIsAssignedDriver() throws Exception {
        Delivery assignedDelivery = buildDelivery(1L, DeliveryStatus.ASSIGNED, 50L);
        Delivery inTransitDelivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);

        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(deliveryService.getById(1L)).thenReturn(assignedDelivery);
        when(profileServiceClient.getDriverProfile(50L)).thenReturn(DRIVER_PROFILE);
        when(deliveryService.updateStatus(1L, DeliveryStatus.IN_TRANSIT)).thenReturn(inTransitDelivery);
        when(deliveryMapper.toDto(inTransitDelivery)).thenReturn(dtoFor(inTransitDelivery));

        mockMvc.perform(post("/deliveries/1/start").with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    void startTransit_returns403_whenCallerIsNotAssignedDriver() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.ASSIGNED, 50L);
        UserSummary stranger = new UserSummary(999L, "Eve Stranger", "eve@example.com");

        when(userServiceClient.findUserByEmail("eve@example.com")).thenReturn(Optional.of(stranger));
        when(deliveryService.getById(1L)).thenReturn(delivery);
        when(profileServiceClient.getDriverProfile(50L)).thenReturn(DRIVER_PROFILE);

        mockMvc.perform(post("/deliveries/1/start").with(user("eve@example.com")))
                .andExpect(status().isForbidden());
    }

    // --- deliver ---

    @Test
    void markDelivered_shouldReturn403_whenCallerIsNotAssignedDriver() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);
        UserSummary stranger = new UserSummary(999L, "Eve Stranger", "eve@example.com");

        when(userServiceClient.findUserByEmail("eve@example.com")).thenReturn(Optional.of(stranger));
        when(deliveryService.getById(1L)).thenReturn(delivery);
        when(profileServiceClient.getDriverProfile(50L)).thenReturn(DRIVER_PROFILE);

        mockMvc.perform(post("/deliveries/1/deliver")
                        .with(user("eve@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"qrCodeToken\": \"any-token\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deliverDelivery_returns200_whenTokenMatches() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);
        Delivery delivered = buildDelivery(1L, DeliveryStatus.DELIVERED, 50L);

        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(deliveryService.getById(1L)).thenReturn(delivery);
        when(profileServiceClient.getDriverProfile(50L)).thenReturn(DRIVER_PROFILE);
        when(deliveryService.completeDelivery(1L, "abc-123-token")).thenReturn(delivered);
        when(deliveryMapper.toDto(delivered)).thenReturn(dtoFor(delivered));

        mockMvc.perform(post("/deliveries/1/deliver")
                        .with(user("charlie@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"qrCodeToken\": \"abc-123-token\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void deliverDelivery_returns400_whenTokenMismatch() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);

        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(deliveryService.getById(1L)).thenReturn(delivery);
        when(profileServiceClient.getDriverProfile(50L)).thenReturn(DRIVER_PROFILE);
        when(deliveryService.completeDelivery(1L, "wrong-token")).thenThrow(new InvalidQrCodeException(1L));

        mockMvc.perform(post("/deliveries/1/deliver")
                        .with(user("charlie@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"qrCodeToken\": \"wrong-token\" }"))
                .andExpect(status().isBadRequest());
    }

    // --- cancel ---

    @Test
    void cancel_shouldReturnCancelledDelivery_whenCallerIsSender() throws Exception {
        Delivery createdDelivery = buildDelivery(1L, DeliveryStatus.CREATED, null);
        Delivery cancelledDelivery = buildDelivery(1L, DeliveryStatus.CANCELLED, null);

        when(userServiceClient.findUserByEmail("alice@example.com")).thenReturn(Optional.of(SENDER));
        when(deliveryService.getById(1L)).thenReturn(createdDelivery);
        when(profileServiceClient.getSenderProfile(1L)).thenReturn(new ProfileSummary(1L, SENDER.id(), SENDER.name()));
        when(deliveryService.updateStatus(1L, DeliveryStatus.CANCELLED)).thenReturn(cancelledDelivery);
        when(deliveryMapper.toDto(cancelledDelivery)).thenReturn(dtoFor(cancelledDelivery));

        mockMvc.perform(post("/deliveries/1/cancel").with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancel_shouldReturn403_whenCallerIsNotSender() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.CREATED, null);
        UserSummary stranger = new UserSummary(999L, "Eve Stranger", "eve@example.com");

        when(userServiceClient.findUserByEmail("eve@example.com")).thenReturn(Optional.of(stranger));
        when(deliveryService.getById(1L)).thenReturn(delivery);
        when(profileServiceClient.getSenderProfile(1L)).thenReturn(new ProfileSummary(1L, SENDER.id(), SENDER.name()));

        mockMvc.perform(post("/deliveries/1/cancel").with(user("eve@example.com")))
                .andExpect(status().isForbidden());
    }

    // --- sent / received ---

    @Test
    void getMySentDeliveries_returnsMappedDtos() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);
        Page<Delivery> page = new PageImpl<>(List.of(delivery), PageRequest.of(0, 20), 1);

        when(userServiceClient.findUserByEmail("alice@example.com")).thenReturn(Optional.of(SENDER));
        when(deliveryService.getSentDeliveries(eq(100L), any(Pageable.class))).thenReturn(page);
        when(deliveryMapper.toDto(delivery)).thenReturn(dtoFor(delivery));

        mockMvc.perform(get("/deliveries/sent").with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getMySentDeliveries_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/deliveries/sent")).andExpect(status().isForbidden());
    }

    @Test
    void getMyReceivedDeliveries_returnsMappedDtos() throws Exception {
        Delivery delivery = buildDelivery(2L, DeliveryStatus.IN_TRANSIT, 50L);
        Page<Delivery> page = new PageImpl<>(List.of(delivery), PageRequest.of(0, 20), 1);

        when(userServiceClient.findUserByEmail("bob@example.com")).thenReturn(Optional.of(RECIPIENT));
        when(deliveryService.getReceivedDeliveries(eq(200L), any(Pageable.class))).thenReturn(page);
        when(deliveryMapper.toDto(delivery)).thenReturn(dtoFor(delivery));

        mockMvc.perform(get("/deliveries/received").with(user("bob@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(2));
    }

    @Test
    void getMyReceivedDeliveries_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/deliveries/received")).andExpect(status().isForbidden());
    }

    // --- driven ---

    @Test
    void getMyDrivenDeliveries_returnsMappedDtos_whenCallerIsDriver() throws Exception {
        Delivery delivery = buildDelivery(3L, DeliveryStatus.IN_TRANSIT, 50L);
        Page<Delivery> page = new PageImpl<>(List.of(delivery), PageRequest.of(0, 20), 1);

        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(profileServiceClient.findDriverProfileByUserId(300L)).thenReturn(Optional.of(DRIVER_PROFILE));
        when(deliveryService.getDrivenDeliveries(eq(50L), any(Pageable.class))).thenReturn(page);
        when(deliveryMapper.toDto(delivery)).thenReturn(dtoFor(delivery));

        mockMvc.perform(get("/deliveries/driven").with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(3));
    }

    @Test
    void getMyDrivenDeliveries_returns404_whenCallerHasNoDriverProfile() throws Exception {
        UserSummary noProfileUser = new UserSummary(400L, "Dana NoProfile", "dana@example.com");
        when(userServiceClient.findUserByEmail("dana@example.com")).thenReturn(Optional.of(noProfileUser));
        when(profileServiceClient.findDriverProfileByUserId(400L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/deliveries/driven").with(user("dana@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyDrivenDeliveries_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/deliveries/driven")).andExpect(status().isForbidden());
    }

    // --- queue ---

    @Test
    void getQueue_returnsMappedDtos_whenCallerIsDriver() throws Exception {
        Delivery delivery = buildDelivery(4L, DeliveryStatus.CREATED, null);
        Page<Delivery> page = new PageImpl<>(List.of(delivery), PageRequest.of(0, 20), 1);

        when(userServiceClient.findUserByEmail("charlie@example.com")).thenReturn(Optional.of(DRIVER));
        when(profileServiceClient.findDriverProfileByUserId(300L)).thenReturn(Optional.of(DRIVER_PROFILE));
        when(deliveryService.getQueueForDriver(eq(50L), eq(0), eq(20))).thenReturn(page);
        when(deliveryMapper.toDto(delivery)).thenReturn(dtoFor(delivery));

        mockMvc.perform(get("/deliveries/queue").with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(4));
    }

    @Test
    void getQueue_returns404_whenCallerHasNoDriverProfile() throws Exception {
        UserSummary noProfileUser = new UserSummary(400L, "Dana NoProfile", "dana@example.com");
        when(userServiceClient.findUserByEmail("dana@example.com")).thenReturn(Optional.of(noProfileUser));
        when(profileServiceClient.findDriverProfileByUserId(400L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/deliveries/queue").with(user("dana@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQueue_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/deliveries/queue")).andExpect(status().isForbidden());
    }

    // --- create delivery ---

    private static final String CREATE_DELIVERY_BODY = """
            {
              "recipientEmail": "bob@example.com",
              "pickUpAddress": {
                "streetNumber": "123", "streetName": "Pickup St",
                "suburb": "Testville", "state": "VIC", "postcode": "3000"
              },
              "dropOffAddress": {
                "streetNumber": "456", "streetName": "Dropoff Ave",
                "suburb": "Testville", "state": "VIC", "postcode": "3000"
              },
              "senderPhoneNumber": "0400111222",
              "recipientPhoneNumber": "0400333444",
              "parcels": [
                { "description": "Box", "lengthCm": 10.00, "widthCm": 10.00, "heightCm": 10.00,
                  "weightKg": 2.00, "declaredValue": 50.00, "insured": false }
              ]
            }
            """;

    @Test
    void createDelivery_returns201WithDeliveryDto_onSuccess() throws Exception {
        Delivery createdDelivery = buildDelivery(5L, DeliveryStatus.CREATED, null);

        when(userServiceClient.findUserByEmail("alice@example.com")).thenReturn(Optional.of(SENDER));
        when(deliveryService.createDelivery(eq(100L), any(CreateDeliveryRequest.class))).thenReturn(createdDelivery);
        when(deliveryMapper.toDto(createdDelivery)).thenReturn(dtoFor(createdDelivery));

        mockMvc.perform(post("/deliveries")
                        .with(user("alice@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_DELIVERY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void createDelivery_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_DELIVERY_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createDelivery_returns404_whenRecipientNotFound() throws Exception {
        when(userServiceClient.findUserByEmail("alice@example.com")).thenReturn(Optional.of(SENDER));
        when(deliveryService.createDelivery(eq(100L), any(CreateDeliveryRequest.class)))
                .thenThrow(new com.hermes.delivery.exception.UserNotFoundException("bob@example.com"));

        mockMvc.perform(post("/deliveries")
                        .with(user("alice@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_DELIVERY_BODY))
                .andExpect(status().isNotFound());
    }

    // --- qrcode ---

    @Test
    void getQrCode_returnsToken_whenCallerIsRecipient() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);
        delivery.setQrCodeToken("abc-123-token");

        when(userServiceClient.findUserByEmail("bob@example.com")).thenReturn(Optional.of(RECIPIENT));
        when(deliveryService.getById(1L)).thenReturn(delivery);
        when(profileServiceClient.getRecipientProfile(2L))
                .thenReturn(new ProfileSummary(2L, RECIPIENT.id(), RECIPIENT.name()));

        mockMvc.perform(get("/deliveries/1/qrcode").with(user("bob@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string("abc-123-token"));
    }

    @Test
    void getQrCode_returnsForbidden_whenCallerIsNotRecipient() throws Exception {
        Delivery delivery = buildDelivery(1L, DeliveryStatus.IN_TRANSIT, 50L);
        delivery.setQrCodeToken("abc-123-token");
        UserSummary stranger = new UserSummary(999L, "Eve Stranger", "eve@example.com");

        when(userServiceClient.findUserByEmail("eve@example.com")).thenReturn(Optional.of(stranger));
        when(deliveryService.getById(1L)).thenReturn(delivery);
        when(profileServiceClient.getRecipientProfile(2L))
                .thenReturn(new ProfileSummary(2L, RECIPIENT.id(), RECIPIENT.name()));

        mockMvc.perform(get("/deliveries/1/qrcode").with(user("eve@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getQrCode_returnsNotFound_whenDeliveryDoesNotExist() throws Exception {
        when(userServiceClient.findUserByEmail("bob@example.com")).thenReturn(Optional.of(RECIPIENT));
        when(deliveryService.getById(99L)).thenThrow(new DeliveryNotFoundException(99L));

        mockMvc.perform(get("/deliveries/99/qrcode").with(user("bob@example.com")))
                .andExpect(status().isNotFound());
    }
}
