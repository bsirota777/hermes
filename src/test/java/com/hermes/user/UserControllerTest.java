package com.hermes.user;

import com.hermes.TestcontainersConfig;
import com.hermes.security.JwtService;
import com.hermes.security.SecurityConfig;
import com.hermes.user.dto.AccountDto;
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
                {"address":"1 New St"}
                """))
                .andExpect(status().isNoContent());

        verify(userService).updateAddress(eq(user), any(UpdateAddressRequest.class));
    }

    @Test
    void updateMyAddress_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/users/me/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":"1 New St"}
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyAddress_returns400_whenAddressBlank() throws Exception {
        mockMvc.perform(patch("/users/me/address")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":""}
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
                .thenReturn(new DriverProfileDto(10L, "1 New St", "0400000000", "LIC123", "ABC123"));

        mockMvc.perform(post("/users/me/driver-profile")
                        .with(user("jdoe@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"address":"1 New St","phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
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
                {"address":"1 New St","phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isForbidden());
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
                {"address":"1 New St","phoneNumber":"0400000000","licenceNumber":"LIC123","vehiclePlate":"ABC123"}
                """))
                .andExpect(status().isConflict());
    }
}