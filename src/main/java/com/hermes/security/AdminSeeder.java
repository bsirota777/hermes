package com.hermes.security;

import com.hermes.user.Role;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${admin.seed.email:admin@hermes.local}") String adminEmail,
                       @Value("${admin.seed.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword.isBlank()) {
            return; // no ADMIN_SEED_PASSWORD set — skip seeding, don't fail startup
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return; // already seeded, don't recreate/overwrite on every restart
        }

        User admin = new User("Admin", adminEmail, passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}