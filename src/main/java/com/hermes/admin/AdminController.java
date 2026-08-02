package com.hermes.admin;

import com.hermes.delivery.Delivery;
import com.hermes.delivery.DeliveryRepository;
import com.hermes.parcel.Parcel;
import com.hermes.parcel.ParcelRepository;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return userRepository.findAll(pageable).map(this::toDto);
    }

    @GetMapping("/deliveries")
    public Page<AdminDeliveryDto> getDeliveries(Pageable pageable) {
        return deliveryRepository.findAllWithDetails(pageable).map(this::toDto);
    }

    @GetMapping("/deliveries/{id}/parcels")
    public ResponseEntity<List<AdminParcelDto>> getParcelsForDelivery(@PathVariable Long id) {
        List<AdminParcelDto> parcels = parcelRepository.findByDeliveryId(id).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(parcels);
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
                user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.isBanned(), user.getCreatedAt());
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