package com.hermes.delivery.client;

import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.FindOrCreateProfileRequest;
import com.hermes.common.profile.ProfileSummary;

import java.util.Optional;

public interface ProfileServiceClient {
    ProfileSummary getSenderProfile(Long senderProfileId);
    ProfileSummary getRecipientProfile(Long recipientProfileId);
    DriverProfileSummary getDriverProfile(Long driverProfileId);

    ProfileSummary findOrCreateSenderProfile(FindOrCreateProfileRequest request);
    ProfileSummary findOrCreateRecipientProfile(FindOrCreateProfileRequest request);

    Optional<ProfileSummary> findSenderProfileByUserId(Long userId);
    Optional<ProfileSummary> findRecipientProfileByUserId(Long userId);
    Optional<DriverProfileSummary> findDriverProfileByUserId(Long userId);
}