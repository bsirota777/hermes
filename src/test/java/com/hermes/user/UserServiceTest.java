package com.hermes.user;

import com.hermes.TestcontainersConfig;
import com.hermes.user.dto.AccountDto;
import com.hermes.user.dto.RegisterRequest;
import com.hermes.user.exception.EmailAlreadyExistsException;
import com.hermes.wallet.Wallet;
import com.hermes.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    private WalletRepository walletRepository;
    @InjectMocks
    UserService userService;

    @Test
    void register_hashesPassword_andSaves() {
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountDto result = userService.registerUser(new RegisterRequest("jdoe", "dup@example.com", "secret"));

        verify(walletRepository).save(any(Wallet.class));
        assertThat(result.email()).isEqualTo("dup@example.com");
        assertThat(result.name()).isEqualTo("jdoe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenEmailAlreadyTaken() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () ->
                userService.registerUser(new RegisterRequest("jdoe", "dup@example.com", "secret")));
    }

    @Test
    void loadUserByUsername_forRegularUser_mapsToRoleUserAuthority() {
        User user = new User("jdoe", "driver@example.com", "hashed-password");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("driver@example.com");

        assertThat(result.getUsername()).isEqualTo("driver@example.com");
        assertThat(result.getPassword()).isEqualTo("hashed-password");

        List<String> authorities = result.getAuthorities().stream().map(Object::toString).toList();
        assertThat(authorities).isEqualTo(List.of("ROLE_USER"));
    }

    @Test
    void loadUserByUsername_forAdminUser_mapsToRoleAdminAuthority() {
        User user = new User("Admin", "admin@example.com", "hashed-password");
        user.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("admin@example.com");

        List<String> authorities = result.getAuthorities().stream().map(Object::toString).toList();
        assertThat(authorities).isEqualTo(List.of("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_withUnknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByUsername("nobody@example.com"));
    }

    @Test
    void loadUserByEmail_withUnknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByEmail("nobody@example.com"));
    }
}