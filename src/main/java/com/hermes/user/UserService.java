package com.hermes.user;

import com.hermes.user.dto.*;
import com.hermes.user.exception.DriverProfileAlreadyExistsException;
import com.hermes.user.exception.EmailAlreadyExistsException;
import com.hermes.wallet.Wallet;
import com.hermes.wallet.WalletRepository;
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
    private final WalletRepository walletRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final SenderProfileRepository senderProfileRepository;
    private final RecipientProfileRepository recipientProfileRepository;

    @Transactional
    public AccountDto registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User(request.name(), request.email(), passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet(savedUser);
        walletRepository.save(wallet);

        return getAccountDetails(savedUser);
    }

    public List<User> searchUsers(String name) {
        if (name == null || name.isBlank()) {
            return userRepository.findAll();
        }
        return userRepository.findByNameContainingIgnoreCase(name);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            return userRepository.save(existingUser);
        });
    }

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
                .build();
    }

    public User loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for this email: " + email));
    }

    public AccountDto getAccountDetails(User user) {
        ContactProfile profile = senderProfileRepository.findByUserId(user.getId())
                .map(p -> (ContactProfile) p)
                .or(() -> recipientProfileRepository.findByUserId(user.getId()).map(p -> (ContactProfile) p))
                .or(() -> driverProfileRepository.findByUserId(user.getId()).map(p -> (ContactProfile) p))
                .orElse(null);

        AddressDto address = profile != null ? AddressDto.from(profile.getAddress()) : null;
        String phoneNumber = profile != null ? profile.getPhoneNumber() : null;

        return new AccountDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt(), address, phoneNumber);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public DriverProfileDto registerAsDriver(User user, DriverRegistrationRequest request) {
        if (driverProfileRepository.existsByUserId(user.getId())) {
            throw new DriverProfileAlreadyExistsException(user.getId());
        }

        DriverProfile profile = new DriverProfile();
        profile.setUser(user);
        profile.setAddress(request.address().toEntity());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setLicenceNumber(request.licenceNumber());
        profile.setVehiclePlate(request.vehiclePlate());

        DriverProfile saved = driverProfileRepository.save(profile);

        return new DriverProfileDto(saved.getId(), AddressDto.from(saved.getAddress()),
                saved.getPhoneNumber(), saved.getLicenceNumber(), saved.getVehiclePlate());
    }

    @Transactional
    public void updateAddress(User user, UpdateAddressRequest request) {
        boolean updatedAny = false;

        updatedAny |= senderProfileRepository.findByUserId(user.getId())
                .map(p -> {
                    p.setAddress(request.address().toEntity());
                    p.setPhoneNumber(request.phoneNumber());
                    senderProfileRepository.save(p);
                    return true;
                })
                .orElse(false);
        updatedAny |= recipientProfileRepository.findByUserId(user.getId())
                .map(p -> {
                    p.setAddress(request.address().toEntity());
                    p.setPhoneNumber(request.phoneNumber());
                    recipientProfileRepository.save(p);
                    return true;
                })
                .orElse(false);
        updatedAny |= driverProfileRepository.findByUserId(user.getId())
                .map(p -> {
                    p.setAddress(request.address().toEntity());
                    p.setPhoneNumber(request.phoneNumber());
                    driverProfileRepository.save(p);
                    return true;
                })
                .orElse(false);

        if (!updatedAny) {
            SenderProfile profile = new SenderProfile();
            profile.setUser(user);
            profile.setAddress(request.address().toEntity());
            profile.setPhoneNumber(request.phoneNumber());
            senderProfileRepository.save(profile);
        }
    }
}
