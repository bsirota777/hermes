package com.hermes.user.admin;

import com.hermes.user.User;
import com.hermes.user.UserRepository;
import com.hermes.user.client.DeliveryServiceClient;
import com.hermes.user.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final DeliveryServiceClient deliveryServiceClient;

    public AdminController(UserRepository userRepository, DeliveryServiceClient deliveryServiceClient) {
        this.userRepository = userRepository;
        this.deliveryServiceClient = deliveryServiceClient;
    }

    @GetMapping("/users")
    public Page<AdminUserDto> getUsers(Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        return userRepository.findAll(sorted).map(this::toDto);
    }

    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<AdminUserDto> setBanned(
            @PathVariable Long id,
            @RequestBody BanRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.banned() && user.getEmail().equals(currentUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins cannot ban themselves");
        }

        user.setBanned(request.banned());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(toDto(saved));
    }

    private AdminUserDto toDto(User user) {
        DeliveryServiceClient.DeliveryCounts counts = deliveryServiceClient.getCountsForUser(user.getId());
        return new AdminUserDto(
                user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.isBanned(),
                counts.sentCount(), counts.receivedCount(), user.getCreatedAt());
    }
}
