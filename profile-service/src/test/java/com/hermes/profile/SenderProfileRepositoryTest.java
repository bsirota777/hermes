package com.hermes.profile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// SenderProfileRepository has no existsByUserId method (unlike DriverProfileRepository) - only
// findByUserId, matching what ProfileService actually calls.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class SenderProfileRepositoryTest {

    @Autowired
    private SenderProfileRepository senderProfileRepository;

    private SenderProfile buildSenderProfile(Long userId) {
        SenderProfile profile = new SenderProfile();
        profile.setUserId(userId);
        profile.setAddress(new Address("123", "Main St", "Springfield", "VIC", "3000"));
        profile.setPhoneNumber("0400000000");
        return profile;
    }

    @Test
    void findByUserId_returnsProfile_whenExists() {
        senderProfileRepository.save(buildSenderProfile(1L));

        Optional<SenderProfile> found = senderProfileRepository.findByUserId(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getAddress().getStreetNumber()).isEqualTo("123");
        assertThat(found.get().getAddress().getStreetName()).isEqualTo("Main St");
        assertThat(found.get().getPhoneNumber()).isEqualTo("0400000000");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoProfileForUser() {
        assertThat(senderProfileRepository.findByUserId(99L)).isEmpty();
    }

    @Test
    void save_violatesUniqueConstraint_whenUserIdDuplicated() {
        senderProfileRepository.saveAndFlush(buildSenderProfile(2L));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> senderProfileRepository.saveAndFlush(buildSenderProfile(2L)));
    }
}
