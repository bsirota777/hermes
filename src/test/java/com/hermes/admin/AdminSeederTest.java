package com.hermes.admin;

import com.hermes.user.Role;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void run_skipsSeeding_whenAdminPasswordBlank() {
        AdminSeeder seeder = new AdminSeeder(userRepository, passwordEncoder, "admin@hermes.local", "");

        seeder.run(applicationArguments);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void run_skipsSeeding_whenAdminAlreadyExists() {
        AdminSeeder seeder = new AdminSeeder(userRepository, passwordEncoder, "admin@hermes.local", "supersecret");

        when(userRepository.existsByEmail("admin@hermes.local")).thenReturn(true);

        seeder.run(applicationArguments);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_createsAdmin_whenPasswordSetAndNotAlreadySeeded() {
        AdminSeeder seeder = new AdminSeeder(userRepository, passwordEncoder, "admin@hermes.local", "supersecret");

        when(userRepository.existsByEmail("admin@hermes.local")).thenReturn(false);
        when(passwordEncoder.encode("supersecret")).thenReturn("hashed-supersecret");

        seeder.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Admin");
        assertThat(saved.getEmail()).isEqualTo("admin@hermes.local");
        assertThat(saved.getPassword()).isEqualTo("hashed-supersecret");
        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void run_isIdempotent_acrossRepeatedInvocations() {
        AdminSeeder seeder = new AdminSeeder(userRepository, passwordEncoder, "admin@hermes.local", "supersecret");

        when(userRepository.existsByEmail("admin@hermes.local"))
                .thenReturn(false)  // first run: not seeded yet
                .thenReturn(true);  // second run: already seeded
        when(passwordEncoder.encode("supersecret")).thenReturn("hashed-supersecret");

        seeder.run(applicationArguments);
        seeder.run(applicationArguments);

        verify(userRepository, times(1)).save(any());
    }
}