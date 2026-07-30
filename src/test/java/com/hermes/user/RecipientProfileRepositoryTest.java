package com.hermes.user;

import com.hermes.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class RecipientProfileRepositoryTest {

    @Autowired
    private RecipientProfileRepository recipientProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        User user = new User("Jane Doe", email, "secret");
        return userRepository.save(user);
    }

    private RecipientProfile buildRecipientProfile(User user) {
        RecipientProfile profile = new RecipientProfile();
        profile.setUser(user);
        profile.setAddress("123 Main St");
        profile.setPhoneNumber("0400000000");
        return profile;
    }

    @Test
    void findByUserId_returnsProfile_whenExists() {
        User user = persistUser("recipient1@example.com");
        RecipientProfile profile = buildRecipientProfile(user);
        recipientProfileRepository.save(profile);

        Optional<RecipientProfile> found = recipientProfileRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAddress()).isEqualTo("123 Main St");
        assertThat(found.get().getPhoneNumber()).isEqualTo("0400000000");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoProfileForUser() {
        User user = persistUser("recipient2@example.com");

        Optional<RecipientProfile> found = recipientProfileRepository.findByUserId(user.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUserId_returnsTrue_whenProfileExists() {
        User user = persistUser("recipient3@example.com");
        RecipientProfile profile = buildRecipientProfile(user);
        recipientProfileRepository.save(profile);

        boolean exists = recipientProfileRepository.existsByUserId(user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserId_returnsFalse_whenNoProfile() {
        User user = persistUser("recipient4@example.com");

        boolean exists = recipientProfileRepository.existsByUserId(user.getId());

        assertThat(exists).isFalse();
    }
}