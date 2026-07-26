package com.hermes.user;

import com.hermes.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
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
        when(userService.createUser(any(User.class)))
                .thenReturn(new User( 1L,"jdoe", "jdoe@example.com", "secret123"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"id": 1, "name":"jdoe","email":"jdoe@example.com","password":"secret123"}
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
}
