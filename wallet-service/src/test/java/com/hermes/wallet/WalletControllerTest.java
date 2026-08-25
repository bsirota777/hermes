package com.hermes.wallet;

import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.StripeAccountNotLinkedException;
import com.hermes.wallet.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// jwt.secret must decode (base64) to >=32 bytes for SecurityConfig's JwtValidator bean -
// throwaway test-only value, never used to sign real tokens.
@WebMvcTest(WalletController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=vTjn89nwZ1y4e1j9w9EgvYynGxHYY9EcvY//zXVsqkU=")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    void getBalance_returnsOkWithBalance() throws Exception {
        when(walletService.getBalance(1L)).thenReturn(new BigDecimal("150.00"));

        mockMvc.perform(get("/wallets/{userId}/balance", 1L).with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void getBalance_returns403_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/wallets/{userId}/balance", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashOut_returnsOkWithUpdatedBalance() throws Exception {
        Wallet wallet = new Wallet(1L);
        wallet.setBalance(new BigDecimal("70.00"));

        when(walletService.cashOut(eq(1L), any(BigDecimal.class))).thenReturn(wallet);

        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .with(user("alice@example.com"))
                        .contentType("application/json")
                        .content("{\"amount\": 30.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void cashOut_returnsBadRequest_whenAmountIsMissing() throws Exception {
        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .with(user("alice@example.com"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cashOut_returnsBadRequest_whenAmountBelowMinimum() throws Exception {
        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .with(user("alice@example.com"))
                        .contentType("application/json")
                        .content("{\"amount\": 5.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cashOut_returnsPaymentRequired_whenInsufficientFunds() throws Exception {
        when(walletService.cashOut(eq(1L), any(BigDecimal.class)))
                .thenThrow(new InsufficientFundsException(10L, new BigDecimal("500.00"), new BigDecimal("70.00")));

        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .with(user("alice@example.com"))
                        .contentType("application/json")
                        .content("{\"amount\": 500.00}"))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void cashOut_returnsConflict_whenStripeAccountNotLinked() throws Exception {
        when(walletService.cashOut(eq(1L), any(BigDecimal.class)))
                .thenThrow(new StripeAccountNotLinkedException(1L));

        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .with(user("alice@example.com"))
                        .contentType("application/json")
                        .content("{\"amount\": 30.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void startOnboarding_returnsOkWithUrl() throws Exception {
        when(walletService.startStripeOnboarding(1L))
                .thenReturn("https://connect.stripe.com/setup/e/acct_x");

        mockMvc.perform(post("/wallets/{userId}/onboarding", 1L).with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://connect.stripe.com/setup/e/acct_x"));
    }
}
