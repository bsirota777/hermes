package com.hermes.wallet.internal;

import com.hermes.common.wallet.CreditWalletRequest;
import com.hermes.common.wallet.WalletTransactionType;
import com.hermes.wallet.WalletService;
import com.hermes.wallet.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /internal/** is permitAll in SecurityConfig (it's only ever called service-to-service,
// not by end users), so unlike WalletControllerTest this needs no .with(user(...)).
@WebMvcTest(InternalWalletController.class)
@ImportAutoConfiguration(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=")
class InternalWalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    void credit_callsWalletServiceAndReturnsOk() throws Exception {
        mockMvc.perform(post("/internal/wallet/credit")
                        .contentType("application/json")
                        .content("""
                                { "userId": 5, "amount": 25.00, "type": "EARNING", "deliveryId": 42 }
                                """))
                .andExpect(status().isOk());

        verify(walletService).credit(5L, new BigDecimal("25.00"), WalletTransactionType.EARNING, 42L);
    }

    @Test
    void credit_worksWithNullDeliveryId() throws Exception {
        mockMvc.perform(post("/internal/wallet/credit")
                        .contentType("application/json")
                        .content("""
                                { "userId": 5, "amount": 25.00, "type": "REFUND", "deliveryId": null }
                                """))
                .andExpect(status().isOk());

        verify(walletService).credit(5L, new BigDecimal("25.00"), WalletTransactionType.REFUND, null);
    }
}
