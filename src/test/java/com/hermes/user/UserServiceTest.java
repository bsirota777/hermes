package com.hermes.user;

import com.hermes.user.exception.EmailAlreadyExistsException;
import com.hermes.wallet.Wallet;
import com.hermes.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
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

        User result = userService.createUser(new User("jdoe", "dup@example.com", "secret"));

        verify(walletRepository).save(any(Wallet.class));
        assertThat(result.getPassword()).isEqualTo("hashed");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenEmailAlreadyTaken() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () ->
                userService.createUser(new User("jdoe", "dup@example.com", "secret")));
    }
}
