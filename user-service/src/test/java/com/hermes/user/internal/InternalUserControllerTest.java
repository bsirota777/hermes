package com.hermes.user.internal;

import com.hermes.common.user.UserSummary;
import com.hermes.user.UserService;
import com.hermes.user.dto.AccountDto;
import com.hermes.user.Role;
import com.hermes.user.security.JwtAuthFilter;
import com.hermes.user.security.JwtService;
import com.hermes.user.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /internal/users/** is permitAll in SecurityConfig (only ever called service-to-service),
// so unlike UserControllerTest this needs no .with(user(...)). SecurityConfig still needs
// JwtAuthFilter/JwtService imported explicitly - see the comment in UserControllerTest for why.
@WebMvcTest(InternalUserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
@TestPropertySource(properties = "jwt.secret=vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=")
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void findByEmail_returns200_whenFound() throws Exception {
        when(userService.findUserByEmail("jdoe@example.com"))
                .thenReturn(Optional.of(new UserSummary(1L, "jdoe", "jdoe@example.com")));

        mockMvc.perform(get("/internal/users/by-email").param("email", "jdoe@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("jdoe"));
    }

    @Test
    void findByEmail_returns404_whenMissing() throws Exception {
        when(userService.findUserByEmail("nobody@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/users/by-email").param("email", "nobody@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returns200_whenFound() throws Exception {
        AccountDto account = new AccountDto(1L, "jdoe", "jdoe@example.com", Role.USER, Instant.now());
        when(userService.getUserById(1L)).thenReturn(Optional.of(account));
        when(userService.toUserSummary(account)).thenReturn(new UserSummary(1L, "jdoe", "jdoe@example.com"));

        mockMvc.perform(get("/internal/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jdoe@example.com"));
    }

    @Test
    void findById_returns404_whenMissing() throws Exception {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/users/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
