package com.hermes.user;

import jakarta.validation.Valid;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User created = userService.createUser(user);
        URI location = URI.create("/users/" + created.getId());
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
}