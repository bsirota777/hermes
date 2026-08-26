package com.hermes.profile;

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

    private RecipientProfile buildRecipientProfile(Long userId) {
        RecipientProfile profile = new RecipientProfile();
        profile.setUserId(userId);
        profile.setAddress(new Address("123", "Main St", "Springfield", "VIC", "3000"));
        profile.setPhoneNumber("0400000000");
        return profile;
    }

    @Test
    void findByUserId_returnsProfile_whenExists() {
        recipientProfileRepository.save(buildRecipientProfile(1L));

        Optional<RecipientProfile> found = recipientProfileRepository.findByUserId(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getAddress().getStreetNumber()).isEqualTo("123");
        assertThat(found.get().getAddress().getStreetName()).isEqualTo("Main St");
        assertThat(found.get().getPhoneNumber()).isEqualTo("0400000000");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoProfileForUser() {
        assertThat(recipientProfileRepository.findByUserId(99L)).isEmpty();
    }

    @Test
    void save_violatesUniqueConstraint_whenUserIdDuplicated() {
        recipientProfileRepository.saveAndFlush(buildRecipientProfile(2L));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> recipientProfileRepository.saveAndFlush(buildRecipientProfile(2L)));
    }
}
