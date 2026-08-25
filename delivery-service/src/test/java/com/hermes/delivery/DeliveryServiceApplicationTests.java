package com.hermes.delivery;

import com.hermes.delivery.client.ProfileServiceClient;
import com.hermes.delivery.client.UserServiceClient;
import com.hermes.delivery.client.WalletServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// The clients are real RestClient-backed beans in production; for a plain context-loads
// smoke test we don't want them making real HTTP calls or needing the other services up.
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class DeliveryServiceApplicationTests {

    @MockitoBean
    private ProfileServiceClient profileServiceClient;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private WalletServiceClient walletServiceClient;

    @Test
    void contextLoads() {
    }
}
