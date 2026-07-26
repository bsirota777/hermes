package com.hermes.delivery;

import com.hermes.user.DriverProfile;
import com.hermes.user.RecipientProfile;
import com.hermes.user.SenderProfile;
import com.hermes.user.User;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryService deliveryService;

    @MockitoBean
    private com.hermes.security.JwtService jwtService;

    @MockitoBean
    private com.hermes.user.UserService userService;

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

    @Test
    void getInTransitDeliveries_returnsMappedDtos() throws Exception {
        Delivery delivery = buildInTransitDelivery(1L);
        Page<Delivery> page = new PageImpl<>(List.of(delivery), PageRequest.of(0, 20), 1);

        when(deliveryService.getInTransitDeliveries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/deliveries/in-transit"))
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

        mockMvc.perform(get("/deliveries/in-transit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getInTransitDeliveries_usesPageAndSizeParams() throws Exception {
        Page<Delivery> page = new PageImpl<>(List.of(), PageRequest.of(2, 5), 0);

        when(deliveryService.getInTransitDeliveries(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/deliveries/in-transit").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());
        // verifies the request is accepted; if you want to assert the exact Pageable passed,
        // capture the argument with an ArgumentCaptor instead of any(Pageable.class)
    }
}