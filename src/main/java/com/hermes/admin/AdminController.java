package com.hermes.admin;

import com.hermes.delivery.Delivery;
import com.hermes.delivery.DeliveryRepository;
import com.hermes.parcel.Parcel;
import com.hermes.parcel.ParcelRepository;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private final ParcelRepository parcelRepository;

    public AdminController(UserRepository userRepository,
                           DeliveryRepository deliveryRepository,
                           ParcelRepository parcelRepository) {
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
        this.parcelRepository = parcelRepository;
    }

    @GetMapping("/users")
    public Page<AdminUserDto> getUsers(Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        return userRepository.findAll(sorted).map(this::toDto);
    }

    @GetMapping("/deliveries")
    public Page<AdminDeliveryDto> getDeliveries(Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        return deliveryRepository.findAllWithDetails(sorted).map(this::toDto);
    }

    @GetMapping("/deliveries/{id}/parcels")
    public ResponseEntity<List<AdminParcelDto>> getParcelsForDelivery(@PathVariable Long id) {
        List<AdminParcelDto> parcels = parcelRepository.findByDeliveryId(id).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(parcels);
    }

    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<AdminUserDto> setBanned(
            @PathVariable Long id,
            @RequestBody BanRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.banned() && user.getEmail().equals(currentUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins cannot ban themselves");
        }

        user.setBanned(request.banned());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toDto(saved));
    }

    private AdminUserDto toDto(User user) {
        long sentCount = deliveryRepository.countBySender_User_Id(user.getId());
        long receivedCount = deliveryRepository.countByRecipient_User_Id(user.getId());
        return new AdminUserDto(
                user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.isBanned(),
                sentCount, receivedCount, user.getCreatedAt());
    }

    private AdminDeliveryDto toDto(Delivery d) {
        var driver = d.getDriver();
        return new AdminDeliveryDto(
                d.getId(), d.getStatus(),
                d.getSender().getUser().getName(), d.getSender().getPhoneNumber(),
                d.getRecipient().getUser().getName(), d.getRecipient().getPhoneNumber(),
                driver != null ? driver.getUser().getName() : null,
                driver != null && driver.getLicenceNumber() != null,
                d.getPickUpAddress(), d.getDropOffAddress(),
                d.getDeliveryFee(), d.getParcels().size(), d.getCreatedAt());
    }

    private AdminParcelDto toDto(Parcel p) {
        return new AdminParcelDto(
                p.getId(), p.getDescription(), p.getWeightKg(),
                p.getDeclaredValue(), p.isInsured(), p.getInsuredValue());
    }
}