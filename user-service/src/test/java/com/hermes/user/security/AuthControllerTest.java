package com.hermes.user.security;

import com.hermes.user.Role;
import com.hermes.user.User;
import com.hermes.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthController authController;

    private User user;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userRepository, passwordEncoder, jwtService);

        user = new User();
        user.setEmail("driver@example.com");
        user.setPassword("hashed-password");
        user.setRole(Role.USER);
        user.setBanned(false);
    }

    @Test
    void login_withValidCredentials_returnsTokenAndOkStatus() {
        LoginRequest request = new LoginRequest("driver@example.com", "plain-password");

        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("driver@example.com", "USER")).thenReturn("fake-jwt-token");

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("fake-jwt-token");
        verify(jwtService).generateToken("driver@example.com", "USER");
    }

    @Test
    void login_withAdminUser_generatesTokenWithAdminRole() {
        user.setRole(Role.ADMIN);
        LoginRequest request = new LoginRequest("driver@example.com", "plain-password");

        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("driver@example.com", "ADMIN")).thenReturn("fake-jwt-token");

        authController.login(request);

        verify(jwtService).generateToken("driver@example.com", "ADMIN");
    }

    @Test
    void login_withUnknownEmail_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("nobody@example.com", "whatever");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("driver@example.com", "wrong-password");

        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_withBannedUser_throwsBadCredentials() {
        user.setBanned(true);
        LoginRequest request = new LoginRequest("driver@example.com", "plain-password");

        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("This account has been banned");

        verifyNoInteractions(jwtService);
    }
}
