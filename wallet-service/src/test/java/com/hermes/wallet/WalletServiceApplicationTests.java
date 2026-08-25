package com.hermes.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.stripe.StripeClient;

// Stripe's own client is mocked out for a plain context-loads smoke test - we don't want
// it making real calls, and it needs a real-looking secret key to construct otherwise.
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class WalletServiceApplicationTests {

    @MockitoBean
    private StripeClient stripeClient;

    @Test
    void contextLoads() {
    }
}
