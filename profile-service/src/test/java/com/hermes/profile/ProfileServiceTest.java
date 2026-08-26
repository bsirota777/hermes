package com.hermes.profile;

import com.hermes.common.address.AddressDto;
import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.FindOrCreateProfileRequest;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.common.user.UserSummary;
import com.hermes.profile.client.UserServiceClient;
import com.hermes.profile.dto.DriverProfileDto;
import com.hermes.profile.dto.DriverRegistrationRequest;
import com.hermes.profile.dto.UpdateAddressRequest;
import com.hermes.profile.exception.DriverProfileAlreadyExistsException;
import com.hermes.profile.exception.DriverProfileNotFoundException;
import com.hermes.profile.exception.ProfileNotFoundException;
import com.hermes.profile.geocoding.Coordinates;
import com.hermes.profile.geocoding.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ProfileService has no old-monolith equivalent - it's new to the split, assembled from what used
// to be direct JPA access on User (driver/sender/recipient profile relations). All cross-service
// name lookups go through UserServiceClient, geocoding through GeocodingService - both mocked here.
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private DriverProfileRepository driverProfiles;
    @Mock
    private SenderProfileRepository senderProfiles;
    @Mock
    private RecipientProfileRepository recipientProfiles;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private GeocodingService geocodingService;

    private ProfileService profileService;

    private static final AddressDto TEST_ADDRESS =
            new AddressDto("1", "New St", "Springfield", "VIC", "3000");
    private static final Coordinates TEST_COORDS = new Coordinates(-37.8, 144.9);

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(driverProfiles, senderProfiles, recipientProfiles, userServiceClient, geocodingService);
    }

    private DriverProfile driverProfile(Long userId) {
        DriverProfile p = new DriverProfile();
        p.setUserId(userId);
        p.setAddress(Address.from(TEST_ADDRESS));
        p.setPhoneNumber("0400000000");
        p.setLicenceNumber("LIC123");
        p.setVehiclePlate("ABC123");
        p.setLatitude(TEST_COORDS.latitude());
        p.setLongitude(TEST_COORDS.longitude());
        return p;
    }

    // ---------- registerDriver ----------

    @Test
    void registerDriver_geocodesAndSaves_whenNoExistingProfile() {
        DriverRegistrationRequest request = new DriverRegistrationRequest(TEST_ADDRESS, "0400000000", "LIC123", "ABC123");
        when(driverProfiles.existsByUserId(1L)).thenReturn(false);
        when(geocodingService.geocode(anyString())).thenReturn(TEST_COORDS);
        when(driverProfiles.save(any(DriverProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverProfileDto result = profileService.registerDriver(1L, request);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.licenceNumber()).isEqualTo("LIC123");
        assertThat(result.latitude()).isEqualTo(-37.8);
        assertThat(result.longitude()).isEqualTo(144.9);
        verify(geocodingService).geocode("1 New St, Springfield VIC 3000, Australia");
    }

    @Test
    void registerDriver_throws_whenProfileAlreadyExists() {
        when(driverProfiles.existsByUserId(1L)).thenReturn(true);

        assertThatThrownBy(() -> profileService.registerDriver(1L,
                new DriverRegistrationRequest(TEST_ADDRESS, "0400000000", "LIC123", "ABC123")))
                .isInstanceOf(DriverProfileAlreadyExistsException.class);

        verify(driverProfiles, never()).save(any());
        verify(geocodingService, never()).geocode(anyString());
    }

    // ---------- updateDriver ----------

    @Test
    void updateDriver_reGeocodes_whenProfileExists() {
        DriverProfile existing = driverProfile(1L);
        AddressDto newAddress = new AddressDto("2", "Other St", "Fitzroy", "VIC", "3065");
        DriverRegistrationRequest request = new DriverRegistrationRequest(newAddress, "0499999999", "LIC999", "XYZ999");
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(geocodingService.geocode(anyString())).thenReturn(new Coordinates(-37.79, 144.98));
        when(driverProfiles.save(any(DriverProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverProfileDto result = profileService.updateDriver(1L, request);

        assertThat(result.licenceNumber()).isEqualTo("LIC999");
        assertThat(result.vehiclePlate()).isEqualTo("XYZ999");
        assertThat(result.latitude()).isEqualTo(-37.79);
        verify(geocodingService).geocode("2 Other St, Fitzroy VIC 3065, Australia");
    }

    @Test
    void updateDriver_throws_whenProfileMissing() {
        when(driverProfiles.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateDriver(99L,
                new DriverRegistrationRequest(TEST_ADDRESS, "0400000000", "LIC123", "ABC123")))
                .isInstanceOf(DriverProfileNotFoundException.class);
    }

    // ---------- getDriver ----------

    @Test
    void getDriver_returnsDto_whenExists() {
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.of(driverProfile(1L)));

        DriverProfileDto result = profileService.getDriver(1L);

        assertThat(result.userId()).isEqualTo(1L);
    }

    @Test
    void getDriver_throws_whenMissing() {
        when(driverProfiles.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getDriver(99L)).isInstanceOf(DriverProfileNotFoundException.class);
    }

    // ---------- updateAddress ----------

    @Test
    void updateAddress_updatesExistingSenderProfile() {
        SenderProfile sender = new SenderProfile();
        sender.setUserId(1L);
        UpdateAddressRequest request = new UpdateAddressRequest(TEST_ADDRESS, "0400000000");
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(recipientProfiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.empty());

        profileService.updateAddress(1L, request);

        assertThat(sender.getPhoneNumber()).isEqualTo("0400000000");
        verify(senderProfiles).save(sender);
    }

    @Test
    void updateAddress_updatesExistingRecipientProfile() {
        RecipientProfile recipient = new RecipientProfile();
        recipient.setUserId(1L);
        UpdateAddressRequest request = new UpdateAddressRequest(TEST_ADDRESS, "0400000000");
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(recipientProfiles.findByUserId(1L)).thenReturn(Optional.of(recipient));
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.empty());

        profileService.updateAddress(1L, request);

        assertThat(recipient.getPhoneNumber()).isEqualTo("0400000000");
        verify(recipientProfiles).save(recipient);
    }

    @Test
    void updateAddress_reGeocodesExistingDriverProfile() {
        DriverProfile driver = driverProfile(1L);
        UpdateAddressRequest request = new UpdateAddressRequest(TEST_ADDRESS, "0411111111");
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(recipientProfiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.of(driver));
        when(geocodingService.geocode(anyString())).thenReturn(TEST_COORDS);

        profileService.updateAddress(1L, request);

        assertThat(driver.getPhoneNumber()).isEqualTo("0411111111");
        verify(geocodingService).geocode(anyString());
        verify(driverProfiles).save(driver);
    }

    @Test
    void updateAddress_updatesAllThreeProfileTypes_whenAllExist() {
        SenderProfile sender = new SenderProfile();
        RecipientProfile recipient = new RecipientProfile();
        DriverProfile driver = driverProfile(1L);
        UpdateAddressRequest request = new UpdateAddressRequest(TEST_ADDRESS, "0400000000");
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(recipientProfiles.findByUserId(1L)).thenReturn(Optional.of(recipient));
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.of(driver));
        when(geocodingService.geocode(anyString())).thenReturn(TEST_COORDS);

        profileService.updateAddress(1L, request);

        verify(senderProfiles).save(sender);
        verify(recipientProfiles).save(recipient);
        verify(driverProfiles).save(driver);
    }

    @Test
    void updateAddress_createsNewSenderProfile_whenNoExistingProfileOfAnyType() {
        UpdateAddressRequest request = new UpdateAddressRequest(TEST_ADDRESS, "0400000000");
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(recipientProfiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.empty());

        profileService.updateAddress(1L, request);

        ArgumentCaptor<SenderProfile> captor = ArgumentCaptor.forClass(SenderProfile.class);
        verify(senderProfiles).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("0400000000");
        verify(geocodingService, never()).geocode(anyString());
    }

    // ---------- findOrCreateSender / findOrCreateRecipient ----------

    @Test
    void findOrCreateSender_returnsExisting_whenProfileAlreadyExists() {
        SenderProfile existing = new SenderProfile();
        existing.setUserId(1L);
        FindOrCreateProfileRequest request = new FindOrCreateProfileRequest(1L, TEST_ADDRESS, "0400000000");
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(userServiceClient.getUser(1L)).thenReturn(new UserSummary(1L, "Jane", "jane@example.com"));

        ProfileSummary result = profileService.findOrCreateSender(request);

        assertThat(result.name()).isEqualTo("Jane");
        verify(senderProfiles, never()).save(any());
    }

    @Test
    void findOrCreateSender_createsProfile_whenNoneExists() {
        FindOrCreateProfileRequest request = new FindOrCreateProfileRequest(1L, TEST_ADDRESS, "0400000000");
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.empty());
        when(senderProfiles.save(any(SenderProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userServiceClient.getUser(1L)).thenReturn(new UserSummary(1L, "Jane", "jane@example.com"));

        ProfileSummary result = profileService.findOrCreateSender(request);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Jane");
        verify(senderProfiles).save(any(SenderProfile.class));
    }

    @Test
    void findOrCreateRecipient_createsProfile_whenNoneExists() {
        FindOrCreateProfileRequest request = new FindOrCreateProfileRequest(2L, TEST_ADDRESS, "0400000000");
        when(recipientProfiles.findByUserId(2L)).thenReturn(Optional.empty());
        when(recipientProfiles.save(any(RecipientProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userServiceClient.getUser(2L)).thenReturn(new UserSummary(2L, "Bob", "bob@example.com"));

        ProfileSummary result = profileService.findOrCreateRecipient(request);

        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Bob");
    }

    // ---------- getSender / getRecipient / getDriverSummary (by profile id) ----------

    @Test
    void getSender_returnsSummary_whenFound() {
        SenderProfile sender = new SenderProfile();
        sender.setUserId(1L);
        when(senderProfiles.findById(10L)).thenReturn(Optional.of(sender));
        when(userServiceClient.getUser(1L)).thenReturn(new UserSummary(1L, "Jane", "jane@example.com"));

        ProfileSummary result = profileService.getSender(10L);

        assertThat(result.name()).isEqualTo("Jane");
    }

    @Test
    void getSender_throws_whenMissing() {
        when(senderProfiles.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getSender(99L)).isInstanceOf(ProfileNotFoundException.class);
        verify(userServiceClient, never()).getUser(any());
    }

    @Test
    void getRecipient_throws_whenMissing() {
        when(recipientProfiles.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getRecipient(99L)).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void getDriverSummary_returnsSummary_whenFound() {
        DriverProfile driver = driverProfile(1L);
        when(driverProfiles.findById(10L)).thenReturn(Optional.of(driver));
        when(userServiceClient.getUser(1L)).thenReturn(new UserSummary(1L, "Driver Dan", "dan@example.com"));

        DriverProfileSummary result = profileService.getDriverSummary(10L);

        assertThat(result.name()).isEqualTo("Driver Dan");
        assertThat(result.latitude()).isEqualTo(-37.8);
    }

    @Test
    void getDriverSummary_throws_whenMissing() {
        // NOTE: getDriverSummary throws ProfileNotFoundException (not DriverProfileNotFoundException)
        // on a missing profile-id lookup, unlike findDriverByUser below which throws
        // DriverProfileNotFoundException for the equivalent by-userId lookup. Inconsistent, but
        // both are already-existing behavior from before this test suite - documented, not changed.
        when(driverProfiles.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getDriverSummary(99L)).isInstanceOf(ProfileNotFoundException.class);
    }

    // ---------- findSenderByUser / findRecipientByUser / findDriverByUser (by userId) ----------

    @Test
    void findSenderByUser_returnsSummary_whenFound() {
        SenderProfile sender = new SenderProfile();
        sender.setUserId(1L);
        when(senderProfiles.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(userServiceClient.getUser(1L)).thenReturn(new UserSummary(1L, "Jane", "jane@example.com"));

        assertThat(profileService.findSenderByUser(1L).name()).isEqualTo("Jane");
    }

    @Test
    void findSenderByUser_throws_whenMissing() {
        when(senderProfiles.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.findSenderByUser(99L)).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void findRecipientByUser_throws_whenMissing() {
        when(recipientProfiles.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.findRecipientByUser(99L)).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void findDriverByUser_returnsSummary_whenFound() {
        DriverProfile driver = driverProfile(1L);
        when(driverProfiles.findByUserId(1L)).thenReturn(Optional.of(driver));
        when(userServiceClient.getUser(1L)).thenReturn(new UserSummary(1L, "Driver Dan", "dan@example.com"));

        DriverProfileSummary result = profileService.findDriverByUser(1L);

        assertThat(result.name()).isEqualTo("Driver Dan");
    }

    @Test
    void findDriverByUser_throws_whenMissing() {
        when(driverProfiles.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.findDriverByUser(99L)).isInstanceOf(DriverProfileNotFoundException.class);
    }
}
