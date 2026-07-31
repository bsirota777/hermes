package com.hermes.payment;

import com.stripe.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Transfer;
import com.stripe.param.TransferCreateParams;
import com.stripe.service.V1Services;

import com.stripe.service.TransferService;
import com.hermes.payment.exception.PayoutFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private StripeClient stripeClient;
    @Mock
    private V1Services v1Services;
    @Mock
    private TransferService transferService;

    private PayoutService payoutService;

    @BeforeEach
    void setUp() {
        payoutService = new PayoutService(stripeClient);
        when(stripeClient.v1()).thenReturn(v1Services);
        when(v1Services.transfers()).thenReturn(transferService);
    }

    @Test
    void sendPayout_returnsTransfer_whenStripeCallSucceeds() throws StripeException {
        Transfer mockTransfer = mock(Transfer.class);
        when(mockTransfer.getId()).thenReturn("tr_test123");
        when(transferService.create(any(TransferCreateParams.class))).thenReturn(mockTransfer);

        Transfer result = payoutService.sendPayout("acct_test123", new BigDecimal("40.00"), "aud", 99L);

        assertThat(result.getId()).isEqualTo("tr_test123");

        verify(transferService).create(argThat(params ->
                params.getAmount() == 4000L &&
                        params.getCurrency().equals("aud") &&
                        params.getDestination().equals("acct_test123")
        ));
    }

    @Test
    void sendPayout_throwsPayoutFailedException_whenStripeCallFails() throws StripeException {
        StripeException stripeException = mock(CardException.class);
        when(transferService.create(any(TransferCreateParams.class))).thenThrow(stripeException);

        assertThatThrownBy(() -> payoutService.sendPayout("acct_test123", new BigDecimal("40.00"), "aud", 99L))
                .isInstanceOf(PayoutFailedException.class)
                .hasCauseInstanceOf(StripeException.class);
    }

    @Test
    void sendPayout_convertsAmountToCentsCorrectly() throws StripeException {
        when(transferService.create(any(TransferCreateParams.class))).thenReturn(mock(Transfer.class));

        payoutService.sendPayout("acct_test123", new BigDecimal("12.34"), "aud", 1L);

        verify(transferService).create(argThat(params -> params.getAmount() == 1234L));
    }
}