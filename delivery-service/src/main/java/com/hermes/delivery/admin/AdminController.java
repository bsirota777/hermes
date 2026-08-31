package com.hermes.delivery.admin;

import com.hermes.delivery.DeliveryRepository;
import com.hermes.delivery.dto.DeliveryDto;
import com.hermes.delivery.exception.DeliveryNotFoundException;
import com.hermes.delivery.mapper.DeliveryMapper;
import com.hermes.delivery.mapper.ParcelMapper;
import com.hermes.delivery.parcel.ParcelRepository;
import com.hermes.delivery.parcel.dto.ParcelResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final DeliveryRepository deliveryRepository;
    private final ParcelRepository parcelRepository;
    private final DeliveryMapper deliveryMapper;

    public AdminController(DeliveryRepository deliveryRepository,
                           ParcelRepository parcelRepository,
                           DeliveryMapper deliveryMapper) {
        this.deliveryRepository = deliveryRepository;
        this.parcelRepository = parcelRepository;
        this.deliveryMapper = deliveryMapper;
    }

    @GetMapping("/deliveries")
    public Page<DeliveryDto> getDeliveries(Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        return deliveryRepository.findAll(sorted).map(deliveryMapper::toDto);
    }

    @GetMapping("/deliveries/{id}/parcels")
    public ResponseEntity<List<ParcelResponseDto>> getParcelsForDelivery(@PathVariable Long id) {
        if (!deliveryRepository.existsById(id)) {
            throw new DeliveryNotFoundException(id);
        }
        List<ParcelResponseDto> parcels = parcelRepository.findByDeliveryId(id).stream()
                .map(ParcelMapper::toDto)
                .toList();
        return ResponseEntity.ok(parcels);
    }
}
