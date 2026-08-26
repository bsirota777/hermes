package com.hermes.user;

import com.hermes.user.dto.AccountDto;
import com.hermes.user.dto.AddressDto;
import com.hermes.user.dto.DriverProfileDto;
import com.hermes.user.dto.DriverRegistrationRequest;
import com.hermes.user.dto.UpdateAddressRequest;
import com.hermes.user.security.JwtAuthFilter;
import com.hermes.user.security.JwtService;
import com.hermes.user.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Unlike wallet/delivery-service (whose SecurityConfig builds JwtAuthFilter from common's classes
// as explicit @Bean methods), user-service's JwtAuthFilter and JwtService are still their own
// @Component-scanned classes, left untouched from before the split. @WebMvcTest's slice only scans
// controller-layer beans, so importing SecurityConfig alone doesn't pull those two in - they have
// to be imported explicitly or SecurityConfig's constructor has no JwtAuthFilter bean to find.
// jwt.secret must decode (base64) to >=32 bytes - JwtService's constructor validates this eagerly.
// Throwaway test-only value, never used to sign real tokens.
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
@TestPropertySource(properties = "jwt.secret=vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final AddressDto TEST_ADDRESS =
            new AddressDto("1", "New St", "Springfield", "VIC", "3000");

    // ---------- create / register ----------

    @Test
    void createUser_returns201_withLocationHeader() throws Exception {
        when(userService.registerUser(any()))
                .thenReturn(new AccountDto(1L, "jdoe", "jdoe@example.com", Role.USER, Instant.now()));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"jdoe","email":"jdoe@example.com","password":"secret123"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/1"))
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

    @Test
    void createUser_isPermittedWithoutAuthentication() throws Exception {
        when(userService.registerUser(any()))
                .thenReturn(new AccountDto(1L, "jdoe", "jdoe@example.com", Role.USER, Instant.now()));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"jdoe","email":"jdoe@example.com","password":"secret123"}
                    """))
                .andExpect(status().isCreated());
    }

    // ---------- search / get / update / delete ----------

    @Test
    void searchUsers_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_returns200_whenFound() throws Exception {
        when(userService.getUserById(1L))
                .thenReturn(java.util.Optional.of(new AccountDto(1L, "jdoe", "jdoe@example.com", Role.USER, Instant.now())));

        mockMvc.perform(get("/users/{id}", 1L).with(user("admin@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jdoe@example.com"));
    }

    @Test
    void getUserById_returns404_whenMissing() throws Exception {
        when(userService.getUserById(99L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/users/{id}", 99L).with(user("admin@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_returns200_onSuccess() throws Exception {
        when(userService.updateUser(eq(1L), any()))
                .thenReturn(java.util.Optional.of(new AccountDto(1L, "Jane Doe", "jdoe@example.com", Role.USER, Instant.now())));

        mockMvc.perform(put("/users/{id}", 1L)
                        .with(user("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"Jane Doe","email":"jdoe@example.com"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void updateUser_returns404_whenMissing() throws Exception {
        when(userService.updateUser(eq(99L), any())).thenReturn(java.util.Optional.empty());

        mockMvc.perform(put("/users/{id}", 99L)
                        .with(user("admin@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"name":"Jane Doe","email":"jdoe@example.com"}
                    """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_returns204_whenDeleted() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(true);

        mockMvc.perform(delete("/users/{id}", 1L).with(user("admin@example.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_returns404_whenMissing() throws Exception {
        when(userService.deleteUser(99L)).thenReturn(false);

        mockMvc.perform(delete("/users/{id}", 99L).with(user("admin@example.com")))
                .andExpect(status().isNotFound());
    }

    // ---------- /me ----------

    @Test
    void getMyAccount_returns200_withCurrentUsersDetails() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);
        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.getAccountDetails(user))
                .thenReturn(new AccountDto(1L, "jdoe", "jdoe@example.com", Role.USER, Instant.now()));

        mockMvc.perform(get("/users/me").with(user("jdoe@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jdoe@example.com"));
    }

    @Test
    void getMyAccount_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeMyPassword_returns204_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);
        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);

        mockMvc.perform(put("/users/me/password")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"currentPassword":"old","newPassword":"new"}
                    """))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(eq(user), any());
    }

    // ---------- address ----------

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

    // ---------- register as driver ----------

    @Test
    void registerAsDriver_returns201_withDriverProfileDto_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.registerAsDriver(eq(user), any(DriverRegistrationRequest.class)))
                .thenReturn(new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123", -37.8, 144.9));

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

    // ---------- get / update driver profile ----------

    @Test
    void getDriverProfile_returns200_withDriverProfileDto_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.getDriverProfile(user))
                .thenReturn(new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0400000000", "LIC123", "ABC123", -37.8, 144.9));

        mockMvc.perform(get("/users/me/driver-profile")
                        .with(user("jdoe@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.licenceNumber").value("LIC123"));
    }

    @Test
    void getDriverProfile_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/users/me/driver-profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateDriverProfile_returns200_withDriverProfileDto_onSuccess() throws Exception {
        User user = new User("jdoe", "jdoe@example.com", "secret");
        user.setId(1L);

        when(userService.loadUserByEmail("jdoe@example.com")).thenReturn(user);
        when(userService.updateDriverProfile(eq(user), any(DriverRegistrationRequest.class)))
                .thenReturn(new DriverProfileDto(10L, 1L, TEST_ADDRESS, "0499999999", "LIC999", "XYZ999", -37.8, 144.9));

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
}
