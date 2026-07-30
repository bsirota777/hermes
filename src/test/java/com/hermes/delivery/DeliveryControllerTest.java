package com.hermes.delivery;

import com.hermes.TestcontainersConfig;
import com.hermes.delivery.dto.DeliveryDto;
import com.hermes.delivery.exception.DeliveryAlreadyAssignedException;
import com.hermes.delivery.exception.DeliveryNotFoundException;
import com.hermes.delivery.exception.InvalidStatusTransitionException;
import com.hermes.delivery.mapper.DeliveryMapper;
import com.hermes.security.SecurityConfig;
import com.hermes.user.DriverProfile;
import com.hermes.user.DriverProfileRepository;
import com.hermes.user.RecipientProfile;
import com.hermes.user.SenderProfile;
import com.hermes.user.User;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@WebMvcTest(DeliveryController.class)
@Import({SecurityConfig.class, TestcontainersConfig.class})
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryService deliveryService;

    @MockitoBean
    private com.hermes.security.JwtService jwtService;

    @MockitoBean
    private com.hermes.user.UserService userService;

    @MockitoBean
    private DriverProfileRepository driverProfileRepository;

    private Delivery buildInTransitDelivery(Long id) {
        User senderUser = new User("Alice Sender", "alice@example.com", "secret");
        senderUser.setId(100L);
        SenderProfile sender = new SenderProfile();
        sender.setUser(senderUser);

        User recipientUser = new User("Bob Recipient", "bob@example.com", "secret");
        recipientUser.setId(200L);
        RecipientProfile recipient = new RecipientProfile();
        recipient.setUser(recipientUser);

        User driverUser = new User("Charlie Driver", "charlie@example.com", "secret");
        driverUser.setId(300L);
        DriverProfile driver = new DriverProfile();
        driver.setUser(driverUser);
        driver.setLicenceNumber("DL111");
        driver.setVehiclePlate("QWE111");

        Delivery delivery = new Delivery();
        delivery.setId(id);
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setCreatedAt(LocalDateTime.of(2026, 7, 26, 10, 0));
        delivery.setSender(sender);
        delivery.setRecipient(recipient);
        delivery.setDriver(driver);

        return delivery;
    }

    // --- existing in-transit tests ---

    @Test
    void getInTransitDeliveries_returnsMappedDtos() throws Exception {
        Delivery delivery = buildInTransitDelivery(1L);
        Page<Delivery> page = new PageImpl<>(List.of(delivery), PageRequest.of(0, 20), 1);

        when(deliveryService.getInTransitDeliveries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/deliveries/in-transit")
                .with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.content[0].senderId").value(100))
                .andExpect(jsonPath("$.content[0].senderName").value("Alice Sender"))
                .andExpect(jsonPath("$.content[0].recipientId").value(200))
                .andExpect(jsonPath("$.content[0].recipientName").value("Bob Recipient"))
                .andExpect(jsonPath("$.content[0].driverId").value(300))
                .andExpect(jsonPath("$.content[0].driverName").value("Charlie Driver"));
    }

    @Test
    void getInTransitDeliveries_returnsEmptyPage_whenNoneInTransit() throws Exception {
        Page<Delivery> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(deliveryService.getInTransitDeliveries(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/deliveries/in-transit")
                .with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getInTransitDeliveries_usesPageAndSizeParams() throws Exception {
        Page<Delivery> page = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0);

        when(deliveryService.getInTransitDeliveries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/deliveries/in-transit").param("page", "2").param("size", "5")
                .with(user("charlie@example.com")))
                .andExpect(status().isOk());
    }

    // --- assign endpoint tests ---

    @Test
    void assignDriver_returnsUpdatedDelivery_onSuccess() throws Exception {
        User driverUser = new User("Charlie Driver", "charlie@example.com", "secret");
        driverUser.setId(300L);

        DriverProfile driverProfile = new DriverProfile();
        driverProfile.setUser(driverUser);

        Delivery assignedDelivery = buildInTransitDelivery(1L);
        assignedDelivery.setStatus(DeliveryStatus.ASSIGNED);
        DeliveryDto expectedDto = DeliveryMapper.toDto(assignedDelivery);

        when(userService.loadUserByEmail("charlie@example.com")).thenReturn(driverUser);
        when(driverProfileRepository.findByUserId(300L)).thenReturn(Optional.of(driverProfile));
        when(deliveryService.assignDriver(eq(1L), eq(driverProfile))).thenReturn(expectedDto);

        mockMvc.perform(post("/deliveries/1/assign")
                .with(user("charlie@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void assignDriver_returns404_whenDeliveryNotFound() throws Exception {
        User driverUser = new User("Charlie Driver", "charlie@example.com", "secret");
        driverUser.setId(300L);

        DriverProfile driverProfile = new DriverProfile();
        driverProfile.setUser(driverUser);

        when(userService.loadUserByEmail("charlie@example.com")).thenReturn(driverUser);
        when(driverProfileRepository.findByUserId(300L)).thenReturn(Optional.of(driverProfile));
        when(deliveryService.assignDriver(eq(99L), eq(driverProfile)))
                .thenThrow(new DeliveryNotFoundException(99L));

        mockMvc.perform(post("/deliveries/99/assign")
                .with(user("charlie@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignDriver_returns409_whenAlreadyAssigned() throws Exception {
        User driverUser = new User("Charlie Driver", "charlie@example.com", "secret");
        driverUser.setId(300L);

        DriverProfile driverProfile = new DriverProfile();
        driverProfile.setUser(driverUser);

        when(userService.loadUserByEmail("charlie@example.com")).thenReturn(driverUser);
        when(driverProfileRepository.findByUserId(300L)).thenReturn(Optional.of(driverProfile));
        when(deliveryService.assignDriver(eq(1L), eq(driverProfile)))
                .thenThrow(new DeliveryAlreadyAssignedException(1L));

        mockMvc.perform(post("/deliveries/1/assign")
                .with(user("charlie@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void assignDriver_returns409_whenInvalidStatusTransition() throws Exception {
        User driverUser = new User("Charlie Driver", "charlie@example.com", "secret");
        driverUser.setId(300L);

        DriverProfile driverProfile = new DriverProfile();
        driverProfile.setUser(driverUser);

        when(userService.loadUserByEmail("charlie@example.com")).thenReturn(driverUser);
        when(driverProfileRepository.findByUserId(300L)).thenReturn(Optional.of(driverProfile));
        when(deliveryService.assignDriver(eq(1L), eq(driverProfile)))
                .thenThrow(new InvalidStatusTransitionException(DeliveryStatus.IN_TRANSIT, DeliveryStatus.ASSIGNED));

        mockMvc.perform(post("/deliveries/1/assign")
                .with(user("charlie@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void assignDriver_returns404_whenDriverProfileNotFound() throws Exception {
        User userWithoutDriverProfile = new User("Dana NoProfile", "dana@example.com", "secret");
        userWithoutDriverProfile.setId(400L);

        when(userService.loadUserByEmail("dana@example.com")).thenReturn(userWithoutDriverProfile);
        when(driverProfileRepository.findByUserId(400L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/deliveries/1/assign")
                .with(user("dana@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignDriver_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/deliveries/1/assign"))
                .andExpect(status().isForbidden());
    }
}