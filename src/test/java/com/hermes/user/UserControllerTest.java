package com.hermes.user;

import com.hermes.TestcontainersConfig;
import com.hermes.security.JwtService;
import com.hermes.security.SecurityConfig;
import com.hermes.user.dto.AccountDto;
import com.hermes.user.dto.AddressDto;
import com.hermes.user.dto.DriverProfileDto;
import com.hermes.user.dto.DriverRegistrationRequest;
import com.hermes.user.dto.UpdateAddressRequest;
import com.hermes.user.exception.DriverProfileAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hermes.user.exception.DriverProfileNotFoundException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, TestcontainersConfig.class})
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(name = "userService")
    UserService userService;

    @MockitoBean
    JwtService jwtService;
    @MockitoBean UserService userDetailsService;

    private static final AddressDto TEST_ADDRESS =
            new AddressDto("1", "New St", "Springfield", "VIC", "3000");

    @Test
    void createUser_returns201_withLocationHeader() throws Exception {
        when(userService.registerUser(any()))
                .thenReturn(new AccountDto(1L, "jdoe", "jdoe@example.com", Role.USER, Instant.now(), null,  null, false));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"jdoe","email":"jdoe@example.com","password":"secret123"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("jdoe"));
    }

    @Test
    void createUser_returns400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"jdoe","email":"not-an-email","password":"secret123"}
                    """))
                .andExpect(status().isBadRequest());
    }

    // --- update address ---

    @Test
    void updateMyAddress_returns204_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);

        mockMvc.perform(patch("/users/me/address")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},
                "phoneNumber":"0400000000"}
                """))
                .andExpect(status().isNoContent());

        verify(userService).updateAddress(eq(user), any(UpdateAddressRequest.class));
    }

    @Test
    void updateMyAddress_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/users/me/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"}}
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyAddress_returns400_whenAddressBlank() throws Exception {
        mockMvc.perform(patch("/users/me/address")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"","streetName":"","suburb":"","state":"","postcode":""},"phoneNumber":"0400000000"}
                """))
                .andExpect(status().isBadRequest());
    }

    // --- register as driver ---

    @Test
    void registerAsDriver_returns201_withDriverProfileDto_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.registerAsDriver(eq(user), any(DriverRegistrationRequest.class)))
                .thenReturn(new DriverProfileDto(10L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123"));

        mockMvc.perform(post("/users/me/driver-profile")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.licenceNumber").value("LIC123"))
                .andExpect(jsonPath("$.vehiclePlate").value("ABC123"));
    }

    @Test
    void registerAsDriver_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/users/me/driver-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerAsDriver_returns400_whenAddressBlank() throws Exception {
        mockMvc.perform(post("/users/me/driver-profile")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {"address":{"streetNumber":"","streetName":"","suburb":"","state":"","postcode":""},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerAsDriver_returns409_whenProfileAlreadyExists() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.registerAsDriver(eq(user), any(DriverRegistrationRequest.class)))
                .thenThrow(new DriverProfileAlreadyExistsException(1L));

        mockMvc.perform(post("/users/me/driver-profile")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isConflict());
    }

    // --- get driver profile ---

    @Test
    void getDriverProfile_returns200_withDriverProfileDto_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.getDriverProfile(user))
                .thenReturn(new DriverProfileDto(10L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123"));

        mockMvc.perform(get("/users/me/driver-profile")
                        .with(user("jdoe@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.licenceNumber").value("LIC123"))
                .andExpect(jsonPath("$.vehiclePlate").value("ABC123"));
    }

    @Test
    void getDriverProfile_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/users/me/driver-profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDriverProfile_returns404_whenProfileMissing() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.getDriverProfile(user))
                .thenThrow(new DriverProfileNotFoundException(1L));

        mockMvc.perform(get("/users/me/driver-profile")
                        .with(user("jdoe@example.com")))
                .andExpect(status().isNotFound());
    }

    // --- update driver profile ---

    @Test
    void updateDriverProfile_returns200_withDriverProfileDto_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.updateDriverProfile(eq(user), any(DriverRegistrationRequest.class)))
                .thenReturn(new DriverProfileDto(10L, TEST_ADDRESS, "0499999999", "LIC999", "XYZ999"));

        mockMvc.perform(patch("/users/me/driver-profile")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0499999999","licenceNumber":"LIC999","vehiclePlate":"XYZ999"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenceNumber").value("LIC999"))
                .andExpect(jsonPath("$.vehiclePlate").value("XYZ999"));
    }

    @Test
    void updateDriverProfile_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/users/me/driver-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0499999999","licenceNumber":"LIC999","vehiclePlate":"XYZ999"}
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateDriverProfile_returns400_whenAddressBlank() throws Exception {
        mockMvc.perform(patch("/users/me/driver-profile")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"","streetName":"","suburb":"","state":"","postcode":""},"phoneNumber":"0499999999","licenceNumber":"LIC999","vehiclePlate":"XYZ999"}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDriverProfile_returns404_whenProfileMissing() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.updateDriverProfile(eq(user), any(DriverRegistrationRequest.class)))
                .thenThrow(new DriverProfileNotFoundException(1L));

        mockMvc.perform(patch("/users/me/driver-profile")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":{"streetNumber":"1","streetName":"New St","suburb":"Springfield","state":"VIC","postcode":"3000"},"phoneNumber":"0499999999","licenceNumber":"LIC999","vehiclePlate":"XYZ999"}
                """))
                .andExpect(status().isNotFound());
    }
}