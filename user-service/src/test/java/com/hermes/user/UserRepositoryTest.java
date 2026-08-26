package com.hermes.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void findByNameContainingIgnoreCase_returnsMatches_caseInsensitively() {
        userRepository.save(new User("jdoe", "jdoe@example.com", "secret"));
        userRepository.save(new User("Jane Doe", "jane@example.com", "secret"));
        userRepository.save(new User("someone else", "other@example.com", "secret"));

        List<User> results = userRepository.findByNameContainingIgnoreCase("doe");

        assertThat(results).hasSize(2);
        assertThat(results).extracting(User::getEmail)
                .containsExactlyInAnyOrder("jdoe@example.com", "jane@example.com");
    }

    @Test
    void findByEmail_returnsUser_whenExists() {
        userRepository.save(new User("jdoe", "jdoe@example.com", "secret"));

        Optional<User> result = userRepository.findByEmail("jdoe@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("jdoe");
    }

    @Test
    void findByEmail_returnsEmpty_whenMissing() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void findByName_returnsUser_whenExists() {
        userRepository.save(new User("jdoe", "jdoe@example.com", "secret"));

        assertThat(userRepository.findByName("jdoe")).isPresent();
    }

    @Test
    void existsByEmail_returnsTrue_whenEmailTaken() {
        userRepository.save(new User("jdoe", "jdoe@example.com", "secret"));

        assertThat(userRepository.existsByEmail("jdoe@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void defaultRoleAndBanned_areUserAndFalse_whenNotSetExplicitly() {
        User saved = userRepository.save(new User("jdoe", "jdoe@example.com", "secret"));

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.USER);
        assertThat(reloaded.isBanned()).isFalse();
    }

    @Test
    void save_violatesUniqueConstraint_whenEmailDuplicated() {
        userRepository.save(new User("jdoe", "dup@example.com", "secret"));

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepository.saveAndFlush(new User("other", "dup@example.com", "secret")));
    }
}
