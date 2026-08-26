package com.hermes.profile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Rewritten from the old monolith's version: DriverProfile no longer has a User JPA relation
// (profile.setUser(user)) - it's a plain userId Long now, so there's no need to persist a User
// row alongside it (user-service owns that table in its own database).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class DriverProfileRepositoryTest {

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    private DriverProfile buildDriverProfile(Long userId) {
        DriverProfile profile = new DriverProfile();
        profile.setUserId(userId);
        profile.setLicenceNumber("DL123456");
        profile.setVehiclePlate("ABC123");
        profile.setAddress(new Address("123", "Main St", "Springfield", "VIC", "3000"));
        profile.setPhoneNumber("0400000000");
        return profile;
    }

    @Test
    void findByUserId_returnsProfile_whenExists() {
        driverProfileRepository.save(buildDriverProfile(1L));

        Optional<DriverProfile> found = driverProfileRepository.findByUserId(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getLicenceNumber()).isEqualTo("DL123456");
        assertThat(found.get().getVehiclePlate()).isEqualTo("ABC123");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoProfileForUser() {
        assertThat(driverProfileRepository.findByUserId(99L)).isEmpty();
    }

    @Test
    void existsByUserId_returnsTrue_whenProfileExists() {
        driverProfileRepository.save(buildDriverProfile(2L));

        assertThat(driverProfileRepository.existsByUserId(2L)).isTrue();
    }

    @Test
    void existsByUserId_returnsFalse_whenNoProfile() {
        assertThat(driverProfileRepository.existsByUserId(99L)).isFalse();
    }

    @Test
    void save_violatesUniqueConstraint_whenUserIdDuplicated() {
        driverProfileRepository.saveAndFlush(buildDriverProfile(3L));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> driverProfileRepository.saveAndFlush(buildDriverProfile(3L)));
    }
}
