package com.hermes.user;

import com.hermes.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // CREATE
    @PostMapping
    public ResponseEntity<AccountDto> createUser(@Valid @RequestBody RegisterRequest request) {
        AccountDto created = userService.registerUser(request);
        URI location = URI.create("/users/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    // RETRIEVE (search by name, partial or full match; omit param for all users)
    @GetMapping
    public List<User> searchUsers(@RequestParam(required = false) String name) {
        return userService.searchUsers(name);
    }

    // RETRIEVE single by id
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @Bean
    CommandLineRunner debugBeans(org.springframework.context.ApplicationContext ctx) {
        return args -> {
            for (String name : ctx.getBeanNamesForType(UserService.class)) {
                System.out.println("UserService bean: " + name);
            }
        };
    }

    @GetMapping("/me")
    public AccountDto getMyAccount(@AuthenticationPrincipal UserDetails currentUser) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        return userService.getAccountDetails(user);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestBody ChangePasswordRequest request) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        userService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/driver-profile")
    public ResponseEntity<DriverProfileDto> registerAsDriver(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody DriverRegistrationRequest request) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        DriverProfileDto profile = userService.registerAsDriver(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PatchMapping("/me/driver-profile")
    public ResponseEntity<DriverProfileDto> updateDriverProfile(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody DriverRegistrationRequest request) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        DriverProfileDto profile = userService.updateDriverProfile(user, request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/driver-profile")
    public ResponseEntity<DriverProfileDto> getDriverProfile(
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = userService.loadUserByEmail(currentUser.getUsername());
        DriverProfileDto profile = userService.getDriverProfile(user);
        return ResponseEntity.ok(profile);
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