package com.hermes.wallet;

import com.hermes.BaseControllerTest;
import com.hermes.TestcontainersConfig;
import com.hermes.user.User;
import com.hermes.wallet.dto.CashOutRequestDto;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;
import com.hermes.exception.GlobalExceptionHandler;
import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@Import(TestcontainersConfig.class)
class WalletControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private WalletService walletService;

    @Test
    void getBalance_returnsOkWithBalance() throws Exception {
        when(walletService.getBalance(1L)).thenReturn(new BigDecimal("150.00"));

        mockMvc.perform(get("/wallets/{userId}/balance", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void getBalance_returnsNotFound_whenWalletDoesNotExist() throws Exception {
        when(walletService.getBalance(99L)).thenThrow(new WalletNotFoundException(99L));

        mockMvc.perform(get("/wallets/{userId}/balance", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void cashOut_returnsOkWithUpdatedBalance() throws Exception {
        User user = new User();
        user.setId(1L);
        Wallet wallet = new Wallet(user);
        wallet.setBalance(new BigDecimal("70.00"));

        when(walletService.cashOut(eq(1L), any(BigDecimal.class))).thenReturn(wallet);

        String requestBody = jsonMapper.writeValueAsString(new CashOutRequestDto(new BigDecimal("30.00")));

        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    void cashOut_returnsBadRequest_whenAmountIsMissing() throws Exception {
        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cashOut_returnsConflictOrPaymentRequired_whenInsufficientFunds() throws Exception {
        when(walletService.cashOut(eq(1L), any(BigDecimal.class)))
                .thenThrow(new InsufficientFundsException(10L, new BigDecimal("500.00"), new BigDecimal("70.00")));

        String requestBody = jsonMapper.writeValueAsString(new CashOutRequestDto(new BigDecimal("500.00")));

        mockMvc.perform(post("/wallets/{userId}/cashout", 1L)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().is4xxClientError()); // adjust to your actual mapped status
    }
}