package com.hermes.user.internal;

import com.hermes.common.user.UserSummary;
import com.hermes.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserSummary> findByEmail(@RequestParam String email) {
        return userService.findUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserSummary> findById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(userService::toUserSummary)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
