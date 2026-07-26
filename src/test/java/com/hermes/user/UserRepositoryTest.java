package com.hermes.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void findByName_returnsUser_whenExists() {
        userRepository.save(new User("jdoe", "jdoe@example.com", "secret"));

        List<User> users = userRepository.findByNameContainingIgnoreCase("jdoe");

        assertThat(users.size()).isEqualTo(1);
    }

    @Test
    void save_violatesUniqueConstraint_whenEmailDuplicated() {
        userRepository.save(new User("jdoe", "dup@example.com", "secret"));

        assertThrows(DataIntegrityViolationException.class, () ->
                userRepository.saveAndFlush(new User("other", "dup@example.com", "secret")));
    }
}
