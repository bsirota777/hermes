package com.hermes.delivery;

import com.hermes.delivery.dto.CompleteDeliveryRequest;
import com.hermes.delivery.dto.CreateDeliveryRequest;
import com.hermes.delivery.dto.DeliveryDto;
import com.hermes.delivery.mapper.DeliveryMapper;
import com.hermes.user.DriverProfile;
import com.hermes.user.DriverProfileRepository;
import com.hermes.user.User;
import com.hermes.user.UserService;
import com.hermes.user.exception.DriverProfileNotFoundException;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<DeliveryDto> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Delivery delivery = deliveryService.createDelivery(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliveryMapper.toDto(delivery));
    }

    @GetMapping("/sent")
    public Page<DeliveryDto> getMySentDeliveries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getSentDeliveries(user.getId(), pageable).map(DeliveryMapper::toDto);
    }

    @GetMapping("/received")
    public Page<DeliveryDto> getMyReceivedDeliveries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getReceivedDeliveries(user.getId(), pageable).map(DeliveryMapper::toDto);
    }

    @GetMapping("/driven")
    public Page<DeliveryDto> getMyDrivenDeliveries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.loadUserByEmail(userDetails.getUsername());
        DriverProfile driver = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new DriverProfileNotFoundException(user.getId()));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return deliveryService.getDrivenDeliveries(driver.getId(), pageable).map(DeliveryMapper::toDto);
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
                                     @Valid @RequestBody CompleteDeliveryRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        // verify the caller is the assigned driver for this delivery
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Delivery delivery = deliveryService.getById(deliveryId);

        if (delivery.getDriver() == null ||
                !delivery.getDriver().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not the assigned driver for this delivery");
        }

        return DeliveryMapper.toDto(deliveryService.completeDelivery(deliveryId, request.qrCodeToken()));
    }

    @PostMapping("/{deliveryId}/cancel")
    public DeliveryDto cancel(@PathVariable Long deliveryId,
                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Delivery delivery = deliveryService.getById(deliveryId);

        if (!delivery.getSender().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not the sender of this delivery");
        }

        return DeliveryMapper.toDto(deliveryService.updateStatus(deliveryId, DeliveryStatus.CANCELLED));
    }

    @GetMapping("/queue")
    public Page<DeliveryDto> getQueue(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.loadUserByEmail(userDetails.getUsername());
        DriverProfile driver = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new DriverProfileNotFoundException(user.getId()));
        return deliveryService.getQueueForDriver(driver, page, size).map(DeliveryMapper::toDto);
    }

    @GetMapping("/{deliveryId}/qrcode")
    public ResponseEntity<String> getQrCode(@PathVariable Long deliveryId, @AuthenticationPrincipal UserDetails userDetails) {
        User caller = userService.loadUserByEmail(userDetails.getUsername());
        Delivery delivery = deliveryService.getById(deliveryId);
        if (!delivery.getRecipient().getUser().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Only the recipient can view this delivery's QR code");
        }
        return ResponseEntity.ok(delivery.getQrCodeToken());
    }

/*    @PostMapping("/{deliveryId}/deliver")
    public ResponseEntity<DeliveryDto> deliverDelivery(
            @PathVariable Long deliveryId,
            @Valid @RequestBody CompleteDeliveryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.loadUserByEmail(userDetails.getUsername());
        Delivery delivery = deliveryService.getById(deliveryId);

        if (delivery.getDriver() == null ||
                !delivery.getDriver().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not the assigned driver for this delivery");
        }

        Delivery result = deliveryService.completeDelivery(deliveryId, request.qrCodeToken());
        return ResponseEntity.ok(DeliveryMapper.toDto(result));
    }*/
}