package com.hermes.profile;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.FindOrCreateProfileRequest;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.common.user.UserSummary;
import com.hermes.profile.client.UserServiceClient;
import com.hermes.profile.dto.DriverProfileDto;
import com.hermes.profile.dto.DriverRegistrationRequest;
import com.hermes.profile.dto.ProfileDto;
import com.hermes.profile.dto.UpdateAddressRequest;
import com.hermes.profile.exception.DriverProfileAlreadyExistsException;
import com.hermes.profile.exception.DriverProfileNotFoundException;
import com.hermes.profile.exception.ProfileNotFoundException;
import com.hermes.profile.geocoding.Coordinates;
import com.hermes.profile.geocoding.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ProfileService {
    private final DriverProfileRepository driverProfiles;
    private final SenderProfileRepository senderProfiles;
    private final RecipientProfileRepository recipientProfiles;
    private final UserServiceClient userServiceClient;
    private final GeocodingService geocodingService;

    @Transactional
    public DriverProfileDto registerDriver(Long userId, DriverRegistrationRequest request) {
        if (driverProfiles.existsByUserId(userId)) throw new DriverProfileAlreadyExistsException(userId);
        DriverProfile p = new DriverProfile();
        p.setUserId(userId); applyDriver(p, request); p = driverProfiles.save(p);
        return toDriverDto(p);
    }

    @Transactional
    public DriverProfileDto updateDriver(Long userId, DriverRegistrationRequest request) {
        DriverProfile p = driverProfiles.findByUserId(userId).orElseThrow(() -> new DriverProfileNotFoundException(userId));
        applyDriver(p, request); return toDriverDto(driverProfiles.save(p));
    }

    public DriverProfileDto getDriver(Long userId) {
        return driverProfiles.findByUserId(userId).map(this::toDriverDto).orElseThrow(() -> new DriverProfileNotFoundException(userId));
    }

    @Transactional
    public void updateAddress(Long userId, UpdateAddressRequest request) {
        boolean updated = senderProfiles.findByUserId(userId).map(p -> { applyContact(p, request); senderProfiles.save(p); return true; }).orElse(false);
        updated |= recipientProfiles.findByUserId(userId).map(p -> { applyContact(p, request); recipientProfiles.save(p); return true; }).orElse(false);
        updated |= driverProfiles.findByUserId(userId).map(p -> { p.setAddress(Address.from(request.address())); p.setPhoneNumber(request.phoneNumber()); geocode(p); driverProfiles.save(p); return true; }).orElse(false);
        if (!updated) {
            SenderProfile p = new SenderProfile(); p.setUserId(userId); applyContact(p, request); senderProfiles.save(p);
        }
    }

    // Mirrors updateAddress's lookup priority (sender, then recipient, then driver) since
    // address/phone are kept in sync across whichever contact profiles exist for the user.
    public ProfileDto getAddress(Long userId) {
        return senderProfiles.findByUserId(userId).map(p -> new ProfileDto(p.getId(), userId, p.getAddress().toDto(), p.getPhoneNumber()))
                .or(() -> recipientProfiles.findByUserId(userId).map(p -> new ProfileDto(p.getId(), userId, p.getAddress().toDto(), p.getPhoneNumber())))
                .or(() -> driverProfiles.findByUserId(userId).map(p -> new ProfileDto(p.getId(), userId, p.getAddress().toDto(), p.getPhoneNumber())))
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }

    @Transactional
    public ProfileSummary findOrCreateSender(FindOrCreateProfileRequest request) {
        SenderProfile p = senderProfiles.findByUserId(request.userId()).orElseGet(() -> createSender(request));
        UserSummary user = userServiceClient.getUser(p.getUserId());
        return new ProfileSummary(p.getId(), p.getUserId(), user.name());
    }

    @Transactional
    public ProfileSummary findOrCreateRecipient(FindOrCreateProfileRequest request) {
        RecipientProfile p = recipientProfiles.findByUserId(request.userId()).orElseGet(() -> createRecipient(request));
        UserSummary user = userServiceClient.getUser(p.getUserId());
        return new ProfileSummary(p.getId(), p.getUserId(), user.name());
    }

    public ProfileSummary getSender(Long profileId) { SenderProfile p = senderProfiles.findById(profileId).orElseThrow(() -> new ProfileNotFoundException(profileId)); UserSummary u=userServiceClient.getUser(p.getUserId()); return new ProfileSummary(p.getId(),p.getUserId(),u.name()); }
    public ProfileSummary getRecipient(Long profileId) { RecipientProfile p = recipientProfiles.findById(profileId).orElseThrow(() -> new ProfileNotFoundException(profileId)); UserSummary u=userServiceClient.getUser(p.getUserId()); return new ProfileSummary(p.getId(),p.getUserId(),u.name()); }
    public DriverProfileSummary getDriverSummary(Long profileId) { DriverProfile p=driverProfiles.findById(profileId).orElseThrow(() -> new ProfileNotFoundException(profileId)); UserSummary u=userServiceClient.getUser(p.getUserId()); return new DriverProfileSummary(p.getId(),p.getUserId(),u.name(),p.getLatitude(),p.getLongitude()); }
    public ProfileSummary findSenderByUser(Long userId) { SenderProfile p=senderProfiles.findByUserId(userId).orElseThrow(() -> new ProfileNotFoundException(userId)); UserSummary u=userServiceClient.getUser(userId); return new ProfileSummary(p.getId(),userId,u.name()); }
    public ProfileSummary findRecipientByUser(Long userId) { RecipientProfile p=recipientProfiles.findByUserId(userId).orElseThrow(() -> new ProfileNotFoundException(userId)); UserSummary u=userServiceClient.getUser(userId); return new ProfileSummary(p.getId(),userId,u.name()); }
    public DriverProfileSummary findDriverByUser(Long userId) { DriverProfile p=driverProfiles.findByUserId(userId).orElseThrow(() -> new DriverProfileNotFoundException(userId)); UserSummary u=userServiceClient.getUser(userId); return new DriverProfileSummary(p.getId(),userId,u.name(),p.getLatitude(),p.getLongitude()); }

    private SenderProfile createSender(FindOrCreateProfileRequest r) { SenderProfile p=new SenderProfile(); p.setUserId(r.userId()); p.setAddress(Address.from(r.address())); p.setPhoneNumber(r.phoneNumber()); return senderProfiles.save(p); }
    private RecipientProfile createRecipient(FindOrCreateProfileRequest r) { RecipientProfile p=new RecipientProfile(); p.setUserId(r.userId()); p.setAddress(Address.from(r.address())); p.setPhoneNumber(r.phoneNumber()); return recipientProfiles.save(p); }
    private void applyContact(ContactProfile p, UpdateAddressRequest r) { p.setAddress(Address.from(r.address())); p.setPhoneNumber(r.phoneNumber()); }
    private void applyDriver(DriverProfile p, DriverRegistrationRequest r) { p.setAddress(Address.from(r.address())); p.setPhoneNumber(r.phoneNumber()); p.setLicenceNumber(r.licenceNumber()); p.setVehiclePlate(r.vehiclePlate()); geocode(p); }
    private void geocode(DriverProfile p) { Coordinates c=geocodingService.geocode(p.getAddress().toFormattedString()); p.setLatitude(c.latitude()); p.setLongitude(c.longitude()); }
    private DriverProfileDto toDriverDto(DriverProfile p) { return new DriverProfileDto(p.getId(),p.getUserId(),p.getAddress().toDto(),p.getPhoneNumber(),p.getLicenceNumber(),p.getVehiclePlate(),p.getLatitude(),p.getLongitude()); }
}
