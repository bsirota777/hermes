package com.hermes.user;

import com.hermes.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WalletRepository walletRepository;

    @BeforeEach
    void cleanUp() {
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullRegistrationFlow_persistsUserAndReturnsLocation() {
        User newUser = new User("jdoe", "jdoe@example.com", "secret123");

        ResponseEntity<User> response = restTemplate.postForEntity("/users", newUser, User.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("jdoe@example.com");
        assertThat(response.getBody().getPassword()).isNotEqualTo("secret123");

        assertThat(userRepository.existsByEmail("jdoe@example.com")).isEqualTo(true);

        // New: verify a wallet was created alongside the user
        Long userId = response.getBody().getId();
        assertThat(walletRepository.findByUserId(userId)).isPresent();
        assertThat(walletRepository.findByUserId(userId).get().getBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registration_returns409_whenEmailAlreadyExists() {
        userRepository.save(new User("existing", "dup@example.com", "hashedpw"));

        User duplicate = new User("newname", "dup@example.com", "secret123");

        ResponseEntity<String> response = restTemplate.postForEntity("/users", duplicate, String.class);

        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());
        System.out.println("Headers: " + response.getHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing@domain", "@nodomain.com", "spaces in@email.com", ""})
    void registration_returns400_whenEmailInvalid(String badEmail) {
        User invalid = new User("jdoe", badEmail, "secret123");

        ResponseEntity<String> response = restTemplate.postForEntity("/users", invalid, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}