package com.hermes.profile;

import com.hermes.profile.client.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class ProfileServiceApplicationTests {

    @MockitoBean
    private UserServiceClient userServiceClient;

    @Test
    void contextLoads() {
    }
}
