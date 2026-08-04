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
class SenderProfileRepositoryTest {

    @Autowired
    private SenderProfileRepository senderProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        User user = new User("Jane Doe", email, "secret");
        return userRepository.save(user);
    }

    private SenderProfile buildSenderProfile(User user) {
        SenderProfile profile = new SenderProfile();
        profile.setUser(user);
        profile.setAddress(new Address("123", "Main St", "Springfield", "VIC", "3000"));
        profile.setPhoneNumber("0400000000");
        return profile;
    }

    @Test
    void findByUserId_returnsProfile_whenExists() {
        User user = persistUser("sender1@example.com");
        SenderProfile profile = buildSenderProfile(user);
        senderProfileRepository.save(profile);

        Optional<SenderProfile> found = senderProfileRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAddress().getStreetNumber()).isEqualTo("123");
        assertThat(found.get().getAddress().getStreetName()).isEqualTo("Main St");
        assertThat(found.get().getPhoneNumber()).isEqualTo("0400000000");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoProfileForUser() {
        User user = persistUser("sender2@example.com");

        Optional<SenderProfile> found = senderProfileRepository.findByUserId(user.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUserId_returnsTrue_whenProfileExists() {
        User user = persistUser("sender3@example.com");
        SenderProfile profile = buildSenderProfile(user);
        senderProfileRepository.save(profile);

        boolean exists = senderProfileRepository.existsByUserId(user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserId_returnsFalse_whenNoProfile() {
        User user = persistUser("sender4@example.com");

        boolean exists = senderProfileRepository.existsByUserId(user.getId());

        assertThat(exists).isFalse();
    }
}