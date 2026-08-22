package com.hermes.user;

import com.hermes.user.dto.AccountDto;
import com.hermes.user.dto.ChangePasswordRequest;
import com.hermes.user.dto.RegisterRequest;
import com.hermes.user.dto.UpdateUserRequest;
import com.hermes.user.dto.DriverProfileDto;
import com.hermes.user.dto.DriverRegistrationRequest;
import com.hermes.user.dto.UpdateAddressRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<AccountDto> createUser(@Valid @RequestBody RegisterRequest request) {
        AccountDto created = userService.registerUser(request);
        return ResponseEntity.created(URI.create("/users/" + created.id())).body(created);
    }

    @GetMapping
    public List<AccountDto> searchUsers(@RequestParam(required = false) String name) {
        return userService.searchUsers(name);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/me")
    public AccountDto getMyAccount(@AuthenticationPrincipal UserDetails currentUser) {
        return userService.getAccountDetails(userService.loadUserByEmail(currentUser.getUsername()));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userService.loadUserByEmail(currentUser.getUsername()), request);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/me/driver-profile")
    public ResponseEntity<DriverProfileDto> registerAsDriver(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody DriverRegistrationRequest request) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerAsDriver(user, request));
    }

    @PatchMapping("/me/driver-profile")
    public ResponseEntity<DriverProfileDto> updateDriverProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody DriverRegistrationRequest request) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        return ResponseEntity.ok(userService.updateDriverProfile(user, request));
    }

    @GetMapping("/me/driver-profile")
    public ResponseEntity<DriverProfileDto> getDriverProfile(
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        return ResponseEntity.ok(userService.getDriverProfile(user));
    }

    @PatchMapping("/me/address")
    public ResponseEntity<Void> updateMyAddress(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody UpdateAddressRequest request) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        userService.updateAddress(user, request);
        return ResponseEntity.noContent().build();
    }

}
