package com.hermes.delivery.parcel;

import com.hermes.delivery.dto.CreateParcelRequestDto;
import com.hermes.delivery.parcel.dto.ParcelResponseDto;
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
    public ResponseEntity<List<ParcelResponseDto>> getParcelsByDelivery(@PathVariable Long deliveryId) {
        List<ParcelResponseDto> parcels = parcelService.getParcelsByDeliveryId(deliveryId).stream()
                .map(ParcelMapper::toDto)
                .toList();
        return ResponseEntity.ok(parcels);
    }

    @GetMapping("/parcels/{id}")
    public ParcelResponseDto getParcelById(@PathVariable Long id) {
        return ParcelMapper.toDto(parcelService.getParcelById(id));
    }

    @PostMapping("/parcels")
    public ResponseEntity<ParcelResponseDto> createParcel(@Valid @RequestBody CreateParcelRequestDto request) {
        Parcel created = parcelService.createParcel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ParcelMapper.toDto(created));
    }
}