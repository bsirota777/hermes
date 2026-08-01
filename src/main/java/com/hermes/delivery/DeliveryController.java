package com.hermes.delivery;

import com.hermes.delivery.dto.DeliveryDto;
import com.hermes.delivery.mapper.DeliveryMapper;
import com.hermes.user.DriverProfile;
import com.hermes.user.DriverProfileRepository;
import com.hermes.user.User;
import com.hermes.user.UserService;
import com.hermes.user.exception.DriverProfileNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final UserService userService;
    private final DriverProfileRepository driverProfileRepository;

    public DeliveryController(DeliveryService deliveryService,
                              UserService userService,
                              DriverProfileRepository driverProfileRepository) {
        this.deliveryService = deliveryService;
        this.userService = userService;
        this.driverProfileRepository = driverProfileRepository;
    }

    @GetMapping("/in-transit")
    public Page<DeliveryDto> getInTransitDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getInTransitDeliveries(pageable)
                .map(DeliveryMapper::toDto); // map entity -> DTO, don't return entities directly from a controller
    }

    @PostMapping("/{deliveryId}/assign")
    public DeliveryDto assignDriver(@PathVariable Long deliveryId,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.loadUserByEmail(userDetails.getUsername());

        DriverProfile driver = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new DriverProfileNotFoundException(user.getId()));

        return DeliveryMapper.toDto(deliveryService.reserve(deliveryId, driver));
    }

    @PostMapping("/{deliveryId}/start")
    public DeliveryDto startTransit(@PathVariable Long deliveryId,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        // verify the caller is the assigned driver for this delivery
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Delivery delivery = deliveryService.getById(deliveryId);

        if (delivery.getDriver() == null ||
                !delivery.getDriver().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not the assigned driver for this delivery");
        }

        return DeliveryMapper.toDto(deliveryService.updateStatus(deliveryId, DeliveryStatus.IN_TRANSIT));
    }

    @PostMapping("/{deliveryId}/deliver")
    public DeliveryDto markDelivered(@PathVariable Long deliveryId,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        // verify the caller is the assigned driver for this delivery
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Delivery delivery = deliveryService.getById(deliveryId);

        if (delivery.getDriver() == null ||
                !delivery.getDriver().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not the assigned driver for this delivery");
        }

        return DeliveryMapper.toDto(deliveryService.updateStatus(deliveryId, DeliveryStatus.DELIVERED));
    }
}