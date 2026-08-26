package com.hermes.user;

import com.hermes.user.client.ProfileServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// ProfileServiceClient is a real RestClient-backed bean in production; for a plain context-loads
// smoke test we don't want it needing profile-service up, so it's mocked out here.
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class UserServiceApplicationTests {

    @MockitoBean
    private ProfileServiceClient profileServiceClient;

    @Test
    void contextLoads() {
    }
}
