package com.hermes.delivery;

import com.hermes.delivery.dto.CreateParcelRequestDto;
import com.hermes.delivery.exception.ParcelNotFoundException;
import com.hermes.security.JwtAuthFilter;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ParcelController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
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

        mockMvc.perform(get("/deliveries/10/parcels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].deliveryId").value(10))
                .andExpect(jsonPath("$[0].weightKg").value(2.50));
    }

    @Test
    void getParcelById_returnsParcel_whenExists() throws Exception {
        when(parcelService.getParcelById(1L)).thenReturn(buildParcel(1L, 10L));

        mockMvc.perform(get("/parcels/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getParcelById_returns404_whenNotFound() throws Exception {
        when(parcelService.getParcelById(99L))
                .thenThrow(new ParcelNotFoundException("Parcel not found with id: 99"));

        mockMvc.perform(get("/parcels/99"))
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
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createParcel_returns400_whenWeightExceedsLimit() throws Exception {
        CreateParcelRequestDto request = new CreateParcelRequestDto();
        request.setDeliveryId(10L);
        request.setLengthCm(new BigDecimal("30.00"));
        request.setWidthCm(new BigDecimal("20.00"));
        request.setHeightCm(new BigDecimal("15.00"));
        request.setWeightKg(new BigDecimal("15.00")); // exceeds 10kg cap
        request.setDeclaredValue(new BigDecimal("100.00"));

        mockMvc.perform(post("/parcels")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}