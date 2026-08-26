package com.hermes.user;

import com.hermes.common.user.UserSummary;
import com.hermes.user.client.ProfileServiceClient;
import com.hermes.user.dto.AccountDto;
import com.hermes.user.dto.AddressDto;
import com.hermes.user.dto.ChangePasswordRequest;
import com.hermes.user.dto.DriverProfileDto;
import com.hermes.user.dto.DriverRegistrationRequest;
import com.hermes.user.dto.RegisterRequest;
import com.hermes.user.dto.UpdateAddressRequest;
import com.hermes.user.dto.UpdateUserRequest;
import com.hermes.user.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Rewritten for the post-split UserService: it now only talks to UserRepository, PasswordEncoder,
// and ProfileServiceClient (a synchronous HTTP client interface) - there's no more direct JPA access
// to driver/sender/recipient profiles here, so registerAsDriver/updateDriverProfile/getDriverProfile
// are tested as simple delegation to the client, not as profile-persistence logic (that now belongs
// to profile-service's own tests).
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ProfileServiceClient profileServiceClient;

    private UserService userService;

    private static final AddressDto TEST_ADDRESS =
            new AddressDto("1", "New St", "Springfield", "VIC", "3000");

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, profileServiceClient);
    }

    // ---------- registerUser ----------

    @Test
    void registerUser_hashesPassword_andSaves() {
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountDto result = userService.registerUser(new RegisterRequest("jdoe", "jdoe@example.com", "secret"));

        assertThat(result.name()).isEqualTo("jdoe");
        assertThat(result.email()).isEqualTo("jdoe@example.com");
        assertThat(result.role()).isEqualTo(Role.USER);

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
    }

    @Test
    void registerUser_throws_whenEmailAlreadyTaken() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(new RegisterRequest("jdoe", "dup@example.com", "secret")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // ---------- searchUsers ----------

    @Test
    void searchUsers_withBlankName_returnsAllUsers() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<AccountDto> result = userService.searchUsers(" ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("jdoe@example.com");
        verify(userRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void searchUsers_withNullName_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of());

        userService.searchUsers(null);

        verify(userRepository).findAll();
        verify(userRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    void searchUsers_withName_filtersByNameContaining() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        when(userRepository.findByNameContainingIgnoreCase("jdoe")).thenReturn(List.of(user));

        List<AccountDto> result = userService.searchUsers("jdoe");

        assertThat(result).hasSize(1);
        verify(userRepository, never()).findAll();
    }

    // ---------- getUserById ----------

    @Test
    void getUserById_returnsDto_whenExists() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<AccountDto> result = userService.getUserById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
    }

    @Test
    void getUserById_returnsEmpty_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.getUserById(99L)).isEmpty();
    }

    // ---------- updateUser ----------

    @Test
    void updateUser_updatesNameAndEmail_whenEmailUnchanged() {
        User existing = new User("jdoe", "jdoe@example.com", "hashed");
        existing.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<AccountDto> result = userService.updateUser(1L, new UpdateUserRequest("Jane Doe", "jdoe@example.com"));

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Jane Doe");
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateUser_updatesEmail_whenNewEmailIsFree() {
        User existing = new User("jdoe", "old@example.com", "hashed");
        existing.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<AccountDto> result = userService.updateUser(1L, new UpdateUserRequest("jdoe", "new@example.com"));

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("new@example.com");
    }

    @Test
    void updateUser_throws_whenNewEmailAlreadyTaken() {
        User existing = new User("jdoe", "old@example.com", "hashed");
        existing.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest("jdoe", "taken@example.com")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_returnsEmpty_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.updateUser(99L, new UpdateUserRequest("jdoe", "jdoe@example.com"))).isEmpty();
    }

    // ---------- deleteUser ----------

    @Test
    void deleteUser_returnsTrue_andDeletes_whenExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThat(userService.deleteUser(1L)).isTrue();
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_returnsFalse_whenMissing() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThat(userService.deleteUser(99L)).isFalse();
        verify(userRepository, never()).deleteById(any());
    }

    // ---------- loadUserByUsername / loadUserByEmail ----------

    @Test
    void loadUserByUsername_forRegularUser_mapsToRoleUserAuthority() {
        User user = new User("jdoe", "driver@example.com", "hashed-password");
        user.setRole(Role.USER);
        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("driver@example.com");

        assertThat(result.getUsername()).isEqualTo("driver@example.com");
        assertThat(result.getPassword()).isEqualTo("hashed-password");
        assertThat(result.isEnabled()).isTrue();
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
    void loadUserByUsername_forBannedUser_isDisabled() {
        User user = new User("jdoe", "banned@example.com", "hashed-password");
        user.setBanned(true);
        when(userRepository.findByEmail("banned@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("banned@example.com");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_withUnknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByEmail_withUnknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByEmail("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByEmail_returnsUser_whenFound() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        when(userRepository.findByEmail("jdoe@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.loadUserByEmail("jdoe@example.com")).isSameAs(user);
    }

    // ---------- getAccountDetails ----------

    @Test
    void getAccountDetails_mapsUserFieldsToDto() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);
        user.setRole(Role.ADMIN);

        AccountDto result = userService.getAccountDetails(user);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("jdoe");
        assertThat(result.email()).isEqualTo("jdoe@example.com");
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    // ---------- changePassword ----------

    @Test
    void changePassword_updatesToNewHashedPassword_whenCurrentPasswordCorrect() {
        User user = new User("jdoe", "jdoe@example.com", "old-hashed");
        when(passwordEncoder.matches("old-plain", "old-hashed")).thenReturn(true);
        when(passwordEncoder.encode("new-plain")).thenReturn("new-hashed");

        userService.changePassword(user, new ChangePasswordRequest("old-plain", "new-plain"));

        assertThat(user.getPassword()).isEqualTo("new-hashed");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_throwsBadCredentials_whenCurrentPasswordIncorrect() {
        User user = new User("jdoe", "jdoe@example.com", "old-hashed");
        when(passwordEncoder.matches("wrong-plain", "old-hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(user, new ChangePasswordRequest("wrong-plain", "new-plain")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getPassword()).isEqualTo("old-hashed");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    // ---------- driver profile delegation ----------

    @Test
    void registerAsDriver_delegatesToProfileServiceClient() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);
        DriverRegistrationRequest request = new DriverRegistrationRequest(TEST_ADDRESS, "0400000000", "LIC123", "ABC123");
        DriverProfileDto expected = new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123", -37.8, 144.9);
        when(profileServiceClient.registerDriver(1L, request)).thenReturn(expected);

        DriverProfileDto result = userService.registerAsDriver(user, request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void updateDriverProfile_delegatesToProfileServiceClient() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);
        DriverRegistrationRequest request = new DriverRegistrationRequest(TEST_ADDRESS, "0499999999", "LIC999", "XYZ999");
        DriverProfileDto expected = new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0499999999", "LIC999", "XYZ999", -37.8, 144.9);
        when(profileServiceClient.updateDriver(1L, request)).thenReturn(expected);

        DriverProfileDto result = userService.updateDriverProfile(user, request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getDriverProfile_delegatesToProfileServiceClient() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);
        DriverProfileDto expected = new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123", -37.8, 144.9);
        when(profileServiceClient.getDriver(1L)).thenReturn(expected);

        assertThat(userService.getDriverProfile(user)).isEqualTo(expected);
    }

    @Test
    void updateAddress_delegatesToProfileServiceClient() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);
        UpdateAddressRequest request = new UpdateAddressRequest(TEST_ADDRESS, "0400000000");

        userService.updateAddress(user, request);

        verify(profileServiceClient).updateAddress(1L, request);
    }

    // ---------- findUserByEmail / toUserSummary ----------

    @Test
    void findUserByEmail_returnsSummary_whenFound() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);
        when(userRepository.findByEmail("jdoe@example.com")).thenReturn(Optional.of(user));

        Optional<UserSummary> result = userService.findUserByEmail("jdoe@example.com");

        assertThat(result).contains(new UserSummary(1L, "jdoe", "jdoe@example.com"));
    }

    @Test
    void findUserByEmail_returnsEmpty_whenMissing() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThat(userService.findUserByEmail("nobody@example.com")).isEmpty();
        verifyNoInteractions(profileServiceClient);
    }

    @Test
    void toUserSummary_fromUser_mapsFields() {
        User user = new User("jdoe", "jdoe@example.com", "hashed");
        user.setId(1L);

        assertThat(userService.toUserSummary(user)).isEqualTo(new UserSummary(1L, "jdoe", "jdoe@example.com"));
    }

    @Test
    void toUserSummary_fromAccountDto_mapsFields() {
        AccountDto dto = new AccountDto(1L, "jdoe", "jdoe@example.com", Role.USER, null);

        assertThat(userService.toUserSummary(dto)).isEqualTo(new UserSummary(1L, "jdoe", "jdoe@example.com"));
    }
}
