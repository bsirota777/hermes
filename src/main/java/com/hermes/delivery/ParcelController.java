package com.hermes.delivery;

import com.hermes.delivery.dto.CreateParcelRequest;
import com.hermes.delivery.dto.ParcelDto;
import com.hermes.delivery.mapper.ParcelMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ParcelController {

    private final ParcelService parcelService;

    public ParcelController(ParcelService parcelService) {
        this.parcelService = parcelService;
    }

    @GetMapping("/deliveries/{deliveryId}/parcels")
    public List<ParcelDto> getParcelsByDelivery(@PathVariable Long deliveryId) {
        return parcelService.getParcelsByDeliveryId(deliveryId).stream()
                .map(ParcelMapper::toDto)
                .toList();
    }

    @GetMapping("/parcels/{id}")
    public ParcelDto getParcelById(@PathVariable Long id) {
        return ParcelMapper.toDto(parcelService.getParcelById(id));
    }

    @PostMapping("/parcels")
    public ResponseEntity<ParcelDto> createParcel(@Valid @RequestBody CreateParcelRequest request) {
        Parcel created = parcelService.createParcel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ParcelMapper.toDto(created));
    }
}