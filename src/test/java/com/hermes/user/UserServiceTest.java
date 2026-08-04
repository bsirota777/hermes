package com.hermes.user;

import com.hermes.TestcontainersConfig;
import com.hermes.user.dto.*;
import com.hermes.user.exception.DriverProfileAlreadyExistsException;
import com.hermes.user.exception.EmailAlreadyExistsException;
import com.hermes.wallet.Wallet;
import com.hermes.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    @Mock
    private DriverProfileRepository driverProfileRepository;
    @Mock
    private SenderProfileRepository senderProfileRepository;
    @Mock
    private RecipientProfileRepository recipientProfileRepository;
    @InjectMocks
    UserService userService;

    private static final AddressDto TEST_ADDRESS =
            new AddressDto("1", "New St", "Springfield", "VIC", "3000");

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

    // ---------- updateAddress ----------

    @Test
    void updateAddress_updatesAllThreeProfiles_whenAllExist() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);

        SenderProfile sender = new SenderProfile();
        RecipientProfile recipient = new RecipientProfile();
        DriverProfile driver = new DriverProfile();

        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(recipientProfileRepository.findByUserId(1L)).thenReturn(Optional.of(recipient));
        when(driverProfileRepository.findByUserId(1L)).thenReturn(Optional.of(driver));

        userService.updateAddress(user, new UpdateAddressRequest(TEST_ADDRESS));

        assertThat(sender.getAddress().getStreetNumber()).isEqualTo("1");
        assertThat(sender.getAddress().getStreetName()).isEqualTo("New St");
        assertThat(sender.getAddress().getSuburb()).isEqualTo("Springfield");
        assertThat(sender.getAddress().getState()).isEqualTo("VIC");
        assertThat(sender.getAddress().getPostcode()).isEqualTo("3000");

        assertThat(recipient.getAddress().getStreetNumber()).isEqualTo("1");
        assertThat(driver.getAddress().getStreetNumber()).isEqualTo("1");

        verify(senderProfileRepository).save(sender);
        verify(recipientProfileRepository).save(recipient);
        verify(driverProfileRepository).save(driver);
    }

    @Test
    void updateAddress_updatesOnlyExistingProfile_whenOthersMissing() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);

        DriverProfile driver = new DriverProfile();

        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(recipientProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(driverProfileRepository.findByUserId(1L)).thenReturn(Optional.of(driver));

        userService.updateAddress(user, new UpdateAddressRequest(TEST_ADDRESS));

        assertThat(driver.getAddress().getStreetNumber()).isEqualTo("1");
        verify(driverProfileRepository).save(driver);
        verify(senderProfileRepository, never()).save(any());
        verify(recipientProfileRepository, never()).save(any());
    }

    @Test
    void updateAddress_doesNothing_whenNoProfilesExist() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);

        when(senderProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(recipientProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(driverProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        userService.updateAddress(user, new UpdateAddressRequest(TEST_ADDRESS));

        verify(senderProfileRepository, never()).save(any());
        verify(recipientProfileRepository, never()).save(any());
        verify(driverProfileRepository, never()).save(any());
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_updatesToNewHashedPassword_whenCurrentPasswordCorrect() {
        User user = new User("jdoe", "jdoe@example.com", "old-hashed");
        user.setId(1L);

        when(passwordEncoder.matches("old-plain", "old-hashed")).thenReturn(true);
        when(passwordEncoder.encode("new-plain")).thenReturn("new-hashed");

        userService.changePassword(user, new ChangePasswordRequest("old-plain", "new-plain"));

        assertThat(user.getPassword()).isEqualTo("new-hashed");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_throwsBadCredentials_whenCurrentPasswordIncorrect() {
        User user = new User("jdoe", "jdoe@example.com", "old-hashed");
        user.setId(1L);

        when(passwordEncoder.matches("wrong-plain", "old-hashed")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                userService.changePassword(user, new ChangePasswordRequest("wrong-plain", "new-plain")));

        assertThat(user.getPassword()).isEqualTo("old-hashed");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    // ---------- registerAsDriver ----------

    @Test
    void registerAsDriver_createsProfile_whenNoneExists() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);

        DriverRegistrationRequest request = new DriverRegistrationRequest(
                TEST_ADDRESS, "0400000000", "LIC123", "ABC123");

        when(driverProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenAnswer(inv -> {
            DriverProfile p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        DriverProfileDto result = userService.registerAsDriver(user, request);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.address().streetNumber()).isEqualTo("1");
        assertThat(result.address().streetName()).isEqualTo("New St");
        assertThat(result.address().suburb()).isEqualTo("Springfield");
        assertThat(result.address().state()).isEqualTo("VIC");
        assertThat(result.address().postcode()).isEqualTo("3000");
        assertThat(result.phoneNumber()).isEqualTo("0400000000");
        assertThat(result.licenceNumber()).isEqualTo("LIC123");
        assertThat(result.vehiclePlate()).isEqualTo("ABC123");

        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    @Test
    void registerAsDriver_throws_whenProfileAlreadyExists() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);

        when(driverProfileRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(DriverProfileAlreadyExistsException.class, () ->
                userService.registerAsDriver(user, new DriverRegistrationRequest(
                        TEST_ADDRESS, "0400000000", "LIC123", "ABC123")));

        verify(driverProfileRepository, never()).save(any());
    }
}