package com.hermes.profile;

import com.hermes.common.address.AddressDto;
import com.hermes.common.profile.DriverProfileSummary;
import com.hermes.common.profile.ProfileSummary;
import com.hermes.profile.dto.DriverProfileDto;
import com.hermes.profile.exception.DriverProfileAlreadyExistsException;
import com.hermes.profile.exception.DriverProfileNotFoundException;
import com.hermes.profile.exception.ProfileNotFoundException;
import com.hermes.profile.geocoding.GeocodingFailedException;
import com.hermes.profile.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /internal is permitAll for the whole controller (see SecurityConfig), so no .with(user(...))
// needed anywhere here - only service-to-service traffic ever hits these endpoints.
@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    private static final AddressDto TEST_ADDRESS =
            new AddressDto("1", "New St", "Springfield", "VIC", "3000");

    @Test
    void registerDriver_returns201_onSuccess() throws Exception {
        when(profileService.registerDriver(eq(1L), any()))
                .thenReturn(new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123", -37.8, 144.9));

        mockMvc.perform(post("/internal/driver-profiles/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.licenceNumber").value("LIC123"));
    }

    @Test
    void registerDriver_returns409_whenAlreadyExists() throws Exception {
        when(profileService.registerDriver(eq(1L), any()))
                .thenThrow(new DriverProfileAlreadyExistsException(1L));

        mockMvc.perform(post("/internal/driver-profiles/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isConflict());
    }

    @Test
    void registerDriver_returns400_whenAddressBlank() throws Exception {
        mockMvc.perform(post("/internal/driver-profiles/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"","streetName":"","suburb":"","state":"","postcode":""},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerDriver_returns502_whenGeocodingFails() throws Exception {
        when(profileService.registerDriver(eq(1L), any()))
                .thenThrow(new GeocodingFailedException("bad address", "No results found"));

        mockMvc.perform(post("/internal/driver-profiles/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isBadGateway());
    }

    @Test
    void updateDriver_returns200_onSuccess() throws Exception {
        when(profileService.updateDriver(eq(1L), any()))
                .thenReturn(new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0499999999", "LIC999", "XYZ999", -37.8, 144.9));

        mockMvc.perform(patch("/internal/driver-profiles/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0499999999","licenceNumber":"LIC999","vehiclePlate":"XYZ999"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenceNumber").value("LIC999"));
    }

    @Test
    void updateDriver_returns404_whenMissing() throws Exception {
        when(profileService.updateDriver(eq(99L), any()))
                .thenThrow(new DriverProfileNotFoundException(99L));

        mockMvc.perform(patch("/internal/driver-profiles/{userId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0499999999","licenceNumber":"LIC999","vehiclePlate":"XYZ999"}
                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDriver_returns200_onSuccess() throws Exception {
        when(profileService.getDriver(1L))
                .thenReturn(new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123", -37.8, 144.9));

        mockMvc.perform(get("/internal/driver-profiles/user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenceNumber").value("LIC123"));
    }

    @Test
    void getDriver_returns404_whenMissing() throws Exception {
        when(profileService.getDriver(99L)).thenThrow(new DriverProfileNotFoundException(99L));

        mockMvc.perform(get("/internal/driver-profiles/user/{userId}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAddress_returns204_onSuccess() throws Exception {
        mockMvc.perform(patch("/internal/profiles/by-user/{userId}/address", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000"}
                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateAddress_returns400_whenAddressBlank() throws Exception {
        mockMvc.perform(patch("/internal/profiles/by-user/{userId}/address", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"","streetName":"","suburb":"","state":"","postcode":""},"phoneNumber":"0400000000"}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void driver_returns200_onSuccess() throws Exception {
        when(profileService.getDriverSummary(10L))
                .thenReturn(new DriverProfileSummary(10L, 1L, "Driver Dan", -37.8, 144.9));

        mockMvc.perform(get("/internal/driver-profiles/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Driver Dan"));
    }

    @Test
    void driver_returns404_whenMissing() throws Exception {
        when(profileService.getDriverSummary(99L)).thenThrow(new ProfileNotFoundException(99L));

        mockMvc.perform(get("/internal/driver-profiles/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void driverByUser_returns200_onSuccess() throws Exception {
        when(profileService.findDriverByUser(1L))
                .thenReturn(new DriverProfileSummary(10L, 1L, "Driver Dan", -37.8, 144.9));

        mockMvc.perform(get("/internal/driver-profiles/by-user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void sender_returns200_onSuccess() throws Exception {
        when(profileService.getSender(10L)).thenReturn(new ProfileSummary(10L, 1L, "Jane"));

        mockMvc.perform(get("/internal/sender-profiles/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane"));
    }

    @Test
    void sender_returns404_whenMissing() throws Exception {
        when(profileService.getSender(99L)).thenThrow(new ProfileNotFoundException(99L));

        mockMvc.perform(get("/internal/sender-profiles/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void recipient_returns200_onSuccess() throws Exception {
        when(profileService.getRecipient(10L)).thenReturn(new ProfileSummary(10L, 2L, "Bob"));

        mockMvc.perform(get("/internal/recipient-profiles/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    void senderByUser_returns200_onSuccess() throws Exception {
        when(profileService.findSenderByUser(1L)).thenReturn(new ProfileSummary(10L, 1L, "Jane"));

        mockMvc.perform(get("/internal/sender-profiles/by-user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane"));
    }

    @Test
    void recipientByUser_returns404_whenMissing() throws Exception {
        when(profileService.findRecipientByUser(99L)).thenThrow(new ProfileNotFoundException(99L));

        mockMvc.perform(get("/internal/recipient-profiles/by-user/{userId}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void senderFindOrCreate_returns200_onSuccess() throws Exception {
        when(profileService.findOrCreateSender(any())).thenReturn(new ProfileSummary(10L, 1L, "Jane"));

        mockMvc.perform(post("/internal/sender-profiles/find-or-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"userId":1,"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane"));
    }

    @Test
    void recipientFindOrCreate_returns200_onSuccess() throws Exception {
        when(profileService.findOrCreateRecipient(any())).thenReturn(new ProfileSummary(11L, 2L, "Bob"));

        mockMvc.perform(post("/internal/recipient-profiles/find-or-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"userId":2,"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }
}
