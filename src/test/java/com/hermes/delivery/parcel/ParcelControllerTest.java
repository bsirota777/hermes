package com.hermes.delivery.parcel;

import com.hermes.delivery.Delivery;
import com.hermes.delivery.dto.CreateParcelRequestDto;
import com.hermes.delivery.exception.ParcelNotFoundException;
import com.hermes.delivery.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Unlike the old version (which excluded JwtAuthFilter from the slice entirely), this imports the
// real SecurityConfig and authenticates requests via .with(user(...)) - matching the pattern used
// for DeliveryControllerTest/WalletControllerTest, so these tests exercise the actual auth behavior
// rather than bypassing it. jwt.secret is a throwaway test-only value, never used to sign real tokens.
@WebMvcTest(ParcelController.class)
@ImportAutoConfiguration(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=")
class ParcelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private ParcelService parcelService;

    private Parcel buildParcel(Long id, Long deliveryId) {
        Delivery delivery = new Delivery();
        delivery.setId(deliveryId);

        Parcel parcel = new Parcel();
        parcel.setId(id);
        parcel.setDelivery(delivery);
        parcel.setDescription("Fragile electronics");
        parcel.setLengthCm(new BigDecimal("30.00"));
        parcel.setWidthCm(new BigDecimal("20.00"));
        parcel.setHeightCm(new BigDecimal("15.00"));
        parcel.setWeightKg(new BigDecimal("2.50"));
        parcel.setDeclaredValue(new BigDecimal("100.00"));
        parcel.setInsured(false);
        return parcel;
    }

    @Test
    void getParcelsByDelivery_returnsMappedList() throws Exception {
        when(parcelService.getParcelsByDeliveryId(10L))
                .thenReturn(List.of(buildParcel(1L, 10L)));

        mockMvc.perform(get("/deliveries/10/parcels").with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].deliveryId").value(10))
                .andExpect(jsonPath("$[0].weightKg").value(2.50));
    }

    @Test
    void getParcelsByDelivery_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/deliveries/10/parcels"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getParcelById_returnsParcel_whenExists() throws Exception {
        when(parcelService.getParcelById(1L)).thenReturn(buildParcel(1L, 10L));

        mockMvc.perform(get("/parcels/1").with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getParcelById_returns404_whenNotFound() throws Exception {
        when(parcelService.getParcelById(99L))
                .thenThrow(new ParcelNotFoundException("Parcel not found with id: 99"));

        mockMvc.perform(get("/parcels/99").with(user("alice@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createParcel_returns201_whenValid() throws Exception {
        CreateParcelRequestDto request = new CreateParcelRequestDto();
        request.setDeliveryId(10L);
        request.setDescription("Fragile electronics");
        request.setLengthCm(new BigDecimal("30.00"));
        request.setWidthCm(new BigDecimal("20.00"));
        request.setHeightCm(new BigDecimal("15.00"));
        request.setWeightKg(new BigDecimal("2.50"));
        request.setDeclaredValue(new BigDecimal("100.00"));
        request.setInsured(false);

        when(parcelService.createParcel(any(CreateParcelRequestDto.class)))
                .thenReturn(buildParcel(1L, 10L));

        mockMvc.perform(post("/parcels")
                        .with(user("alice@example.com"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createParcel_returns400_whenWeightExceedsLimit() throws Exception {
        CreateParcelRequestDto request = new CreateParcelRequestDto();
        request.setDeliveryId(10L);
        request.setDescription("Heavy parcel");
        request.setLengthCm(new BigDecimal("30.00"));
        request.setWidthCm(new BigDecimal("20.00"));
        request.setHeightCm(new BigDecimal("15.00"));
        request.setWeightKg(new BigDecimal("15.00")); // exceeds 10kg cap
        request.setDeclaredValue(new BigDecimal("100.00"));

        mockMvc.perform(post("/parcels")
                        .with(user("alice@example.com"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createParcel_returns403_whenUnauthenticated() throws Exception {
        CreateParcelRequestDto request = new CreateParcelRequestDto();
        request.setDeliveryId(10L);
        request.setDescription("Fragile electronics");
        request.setLengthCm(new BigDecimal("30.00"));
        request.setWidthCm(new BigDecimal("20.00"));
        request.setHeightCm(new BigDecimal("15.00"));
        request.setWeightKg(new BigDecimal("2.50"));
        request.setDeclaredValue(new BigDecimal("100.00"));

        mockMvc.perform(post("/parcels")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
