package com.hermes.user;

import com.hermes.user.dto.AccountDto;
import com.hermes.user.dto.ChangePasswordRequest;
import com.hermes.user.dto.RegisterRequest;
import com.hermes.user.dto.UpdateUserRequest;
import com.hermes.user.dto.UpdateAddressRequest;
import com.hermes.user.dto.DriverProfileDto;
import com.hermes.user.dto.DriverRegistrationRequest;
import com.hermes.user.client.ProfileServiceClient;
import com.hermes.user.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileServiceClient profileServiceClient;

    @Transactional
    public AccountDto registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User(request.name(), request.email(), passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);
        return toAccountDto(savedUser);
    }

    public List<AccountDto> searchUsers(String name) {
        List<User> users = name == null || name.isBlank()
                ? userRepository.findAll()
                : userRepository.findByNameContainingIgnoreCase(name);
        return users.stream().map(this::toAccountDto).toList();
    }

    public Optional<AccountDto> getUserById(Long id) {
        return userRepository.findById(id).map(this::toAccountDto);
    }

    @Transactional
    public Optional<AccountDto> updateUser(Long id, UpdateUserRequest request) {
        return userRepository.findById(id).map(existingUser -> {
            if (!existingUser.getEmail().equalsIgnoreCase(request.email())
                    && userRepository.existsByEmail(request.email())) {
                throw new EmailAlreadyExistsException(request.email());
            }
            existingUser.setName(request.name());
            existingUser.setEmail(request.email());
            return toAccountDto(userRepository.save(existingUser));
        });
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) {
        User user = loadUserByEmail(email);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .disabled(user.isBanned())
                .build();
    }

    public User loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for this email: " + email));
    }

    public AccountDto getAccountDetails(User user) {
        return toAccountDto(user);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }


    public DriverProfileDto registerAsDriver(User user, DriverRegistrationRequest request) {
        return profileServiceClient.registerDriver(user.getId(), request);
    }

    public DriverProfileDto updateDriverProfile(User user, DriverRegistrationRequest request) {
        return profileServiceClient.updateDriver(user.getId(), request);
    }

    public DriverProfileDto getDriverProfile(User user) {
        return profileServiceClient.getDriver(user.getId());
    }

    public void updateAddress(User user, UpdateAddressRequest request) {
        profileServiceClient.updateAddress(user.getId(), request);
    }

    public Optional<com.hermes.common.user.UserSummary> findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new com.hermes.common.user.UserSummary(user.getId(), user.getName(), user.getEmail()));
    }

    public com.hermes.common.user.UserSummary toUserSummary(User user) {
        return new com.hermes.common.user.UserSummary(user.getId(), user.getName(), user.getEmail());
    }

    public com.hermes.common.user.UserSummary toUserSummary(AccountDto accountDto) {
        return new com.hermes.common.user.UserSummary(accountDto.id(), accountDto.name(), accountDto.email());
    }

    private AccountDto toAccountDto(User user) {
        return new AccountDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
