package com.hermes.parcel;

import com.hermes.TestcontainersConfig;
import com.hermes.delivery.Delivery;
import com.hermes.delivery.DeliveryRepository;
import com.hermes.delivery.dto.CreateParcelRequestDto;
import com.hermes.delivery.exception.ParcelNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Import(TestcontainersConfig.class)
class ParcelServiceTest {

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    private ParcelService parcelService;

    @BeforeEach
    void setUp() {
        parcelService = new ParcelService(parcelRepository, deliveryRepository);
    }

    private CreateParcelRequestDto buildValidRequest(Long deliveryId) {
        CreateParcelRequestDto request = new CreateParcelRequestDto();
        request.setDeliveryId(deliveryId);
        request.setLengthCm(new BigDecimal("30.00"));
        request.setWidthCm(new BigDecimal("20.00"));
        request.setHeightCm(new BigDecimal("15.00"));
        request.setWeightKg(new BigDecimal("2.50"));
        request.setDeclaredValue(new BigDecimal("100.00"));
        request.setInsured(false);
        return request;
    }

    @Test
    void getParcelsByDeliveryId_returnsParcelsFromRepository() {
        Parcel parcel = new Parcel();
        when(parcelRepository.findByDeliveryId(1L)).thenReturn(List.of(parcel));

        List<Parcel> result = parcelService.getParcelsByDeliveryId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getParcelById_returnsParcel_whenExists() {
        Parcel parcel = new Parcel();
        parcel.setId(5L);
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcel));

        Parcel result = parcelService.getParcelById(5L);

        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void getParcelById_throwsParcelNotFound_whenMissing() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parcelService.getParcelById(99L))
                .isInstanceOf(ParcelNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createParcel_savesParcel_whenDeliveryExists() {
        Delivery delivery = new Delivery();
        delivery.setId(10L);
        CreateParcelRequestDto request = buildValidRequest(10L);

        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(delivery));
        when(parcelRepository.save(any(Parcel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Parcel result = parcelService.createParcel(request);

        assertThat(result.getDelivery()).isEqualTo(delivery);
        assertThat(result.getWeightKg()).isEqualByComparingTo("2.50");
    }

    @Test
    void createParcel_throwsNotFound_whenDeliveryMissing() {
        CreateParcelRequestDto request = buildValidRequest(404L);
        when(deliveryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parcelService.createParcel(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}