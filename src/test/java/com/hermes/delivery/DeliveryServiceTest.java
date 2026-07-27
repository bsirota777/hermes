package com.hermes.delivery;

import com.hermes.delivery.exception.InvalidDeliveryException;
import com.hermes.user.*;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    private DeliveryService deliveryService;

    private SenderProfileRepository senderProfileRepository;
    private RecipientProfileRepository recipientProfileRepository;

    @Mock
    private ProducerTemplate producerTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(deliveryRepository, producerTemplate,
                senderProfileRepository, recipientProfileRepository);
    }

    private User buildUser(Long id, String email) {
        User user = new User("Test User", email, "secret");
        user.setId(id);
        return user;
    }

    private SenderProfile buildSender(Long userId, String email) {
        SenderProfile sender = new SenderProfile();
        sender.setUser(buildUser(userId, email));
        return sender;
    }

    private RecipientProfile buildRecipient(Long userId, String email) {
        RecipientProfile recipient = new RecipientProfile();
        recipient.setUser(buildUser(userId, email));
        return recipient;
    }

    @Test
    void createDelivery_savesDelivery_whenSenderAndRecipientDifferent() {
        SenderProfile sender = buildSender(1L, "sender@example.com");
        RecipientProfile recipient = buildRecipient(2L, "recipient@example.com");

        when(deliveryRepository.save(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Delivery result = deliveryService.createDelivery(sender, recipient);

        assertThat(result.getSender()).isEqualTo(sender);
        assertThat(result.getRecipient()).isEqualTo(recipient);
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    void createDelivery_throws_whenSenderAndRecipientAreSameUser() {
        SenderProfile sender = buildSender(1L, "same@example.com");
        RecipientProfile recipient = buildRecipient(1L, "same@example.com");

        assertThatThrownBy(() -> deliveryService.createDelivery(sender, recipient))
                .isInstanceOf(InvalidDeliveryException.class)
                .hasMessageContaining("Sender and recipient cannot be the same user");
    }

    @Test
    void getInTransitDeliveries_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Delivery delivery = new Delivery();
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        Page<Delivery> expectedPage = new PageImpl<>(List.of(delivery), pageable, 1);

        when(deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable))
                .thenReturn(expectedPage);

        Page<Delivery> result = deliveryService.getInTransitDeliveries(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
    }
}