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
class DriverProfileRepositoryTest {

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        User user = new User("Jane Doe", email, "secret");
        return userRepository.save(user);
    }

    private DriverProfile buildDriverProfile(User user) {
        DriverProfile profile = new DriverProfile();
        profile.setUser(user);
        profile.setLicenceNumber("DL123456");
        profile.setVehiclePlate("ABC123");
        profile.setAddress("123 Main St");
        profile.setPhoneNumber("0400000000");
        return profile;
    }

    @Test
    void findByUserId_returnsProfile_whenExists() {
        User user = persistUser("driver1@example.com");
        DriverProfile profile = buildDriverProfile(user);
        driverProfileRepository.save(profile);

        Optional<DriverProfile> found = driverProfileRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getLicenceNumber()).isEqualTo("DL123456");
        assertThat(found.get().getVehiclePlate()).isEqualTo("ABC123");
    }

    @Test
    void findByUserId_returnsEmpty_whenNoProfileForUser() {
        User user = persistUser("driver2@example.com");

        Optional<DriverProfile> found = driverProfileRepository.findByUserId(user.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUserId_returnsTrue_whenProfileExists() {
        User user = persistUser("driver3@example.com");
        DriverProfile profile = buildDriverProfile(user);
        driverProfileRepository.save(profile);

        boolean exists = driverProfileRepository.existsByUserId(user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserId_returnsFalse_whenNoProfile() {
        User user = persistUser("driver4@example.com");

        boolean exists = driverProfileRepository.existsByUserId(user.getId());

        assertThat(exists).isFalse();
    }
}