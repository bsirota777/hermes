package com.hermes.profile;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.FindOrCreateProfileRequest;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.profile.dto.DriverProfileDto;
import com.hermes.profile.dto.DriverRegistrationRequest;
import com.hermes.profile.dto.ProfileDto;
import com.hermes.profile.dto.UpdateAddressRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/internal") @RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;

    @PostMapping("/driver-profiles/{userId}")
    public ResponseEntity<DriverProfileDto> registerDriver(@PathVariable Long userId, @Valid @RequestBody DriverRegistrationRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.registerDriver(userId,request)); }
    @PatchMapping("/driver-profiles/{userId}")
    public DriverProfileDto updateDriver(@PathVariable Long userId, @Valid @RequestBody DriverRegistrationRequest request) { return service.updateDriver(userId,request); }
    @GetMapping("/driver-profiles/user/{userId}")
    public DriverProfileDto getDriver(@PathVariable Long userId) { return service.getDriver(userId); }
    @PatchMapping("/profiles/by-user/{userId}/address")
    public ResponseEntity<Void> updateAddress(@PathVariable Long userId, @Valid @RequestBody UpdateAddressRequest request) { service.updateAddress(userId,request); return ResponseEntity.noContent().build(); }
    @GetMapping("/profiles/by-user/{userId}")
    public ProfileDto getAddress(@PathVariable Long userId) { return service.getAddress(userId); }

    @GetMapping("/driver-profiles/{id}") public DriverProfileSummary driver(@PathVariable Long id) { return service.getDriverSummary(id); }
    @GetMapping("/driver-profiles/by-user/{userId}") public DriverProfileSummary driverByUser(@PathVariable Long userId) { return service.findDriverByUser(userId); }
    @GetMapping("/sender-profiles/{id}") public ProfileSummary sender(@PathVariable Long id) { return service.getSender(id); }
    @GetMapping("/recipient-profiles/{id}") public ProfileSummary recipient(@PathVariable Long id) { return service.getRecipient(id); }
    @GetMapping("/sender-profiles/by-user/{userId}") public ProfileSummary senderByUser(@PathVariable Long userId) { return service.findSenderByUser(userId); }
    @GetMapping("/recipient-profiles/by-user/{userId}") public ProfileSummary recipientByUser(@PathVariable Long userId) { return service.findRecipientByUser(userId); }
    @PostMapping("/sender-profiles/find-or-create") public ProfileSummary senderFindOrCreate(@RequestBody FindOrCreateProfileRequest request) { return service.findOrCreateSender(request); }
    @PostMapping("/recipient-profiles/find-or-create") public ProfileSummary recipientFindOrCreate(@RequestBody FindOrCreateProfileRequest request) { return service.findOrCreateRecipient(request); }
}
