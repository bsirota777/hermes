package com.hermes.delivery;

import com.hermes.delivery.dto.DeliveryRequestDto;
import com.hermes.delivery.exception.InvalidDeliveryException;
import com.hermes.user.RecipientProfile;
import com.hermes.user.RecipientProfileRepository;
import com.hermes.user.SenderProfile;
import com.hermes.user.SenderProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.apache.camel.ProducerTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final ProducerTemplate producerTemplate;
    private final SenderProfileRepository senderProfileRepository;
    private final RecipientProfileRepository recipientProfileRepository;

    public DeliveryService(DeliveryRepository deliveryRepository, ProducerTemplate producerTemplate,
                           SenderProfileRepository senderProfileRepository, RecipientProfileRepository recipientProfileRepository) {
        this.deliveryRepository = deliveryRepository;
        this.producerTemplate = producerTemplate;
        this.senderProfileRepository = senderProfileRepository;
        this.recipientProfileRepository = recipientProfileRepository;
    }

    @Transactional
    public Delivery createDeliveryRequest(DeliveryRequestDto request) {
        SenderProfile sender = senderProfileRepository.findById(request.senderProfileId())
                .orElseThrow(() -> new EntityNotFoundException("Sender profile not found: " + request.senderProfileId()));
        RecipientProfile recipient = recipientProfileRepository.findById(request.recipientProfileId())
                .orElseThrow(() -> new EntityNotFoundException("Recipient profile not found: " + request.recipientProfileId()));

        Delivery delivery = new Delivery();
        delivery.setSender(sender);
        delivery.setRecipient(recipient);
        delivery.setPickUpAddress(request.pickUpAddress());
        delivery.setDropOffAddress(request.dropOffAddress());
        delivery.setStatus(DeliveryStatus.CREATED);

        Delivery saved = deliveryRepository.save(delivery);
        producerTemplate.sendBody("seda:delivery-requests", saved);

        return saved;
    }

    public Delivery createDelivery(SenderProfile sender, RecipientProfile recipient) {
        validateSenderNotRecipient(sender, recipient);

        Delivery delivery = new Delivery();
        delivery.setSender(sender);
        delivery.setRecipient(recipient);
        // set other fields...

        return deliveryRepository.save(delivery);
    }

    private void validateSenderNotRecipient(SenderProfile sender, RecipientProfile recipient) {
        if (sender.getUser().getId().equals(recipient.getUser().getId())) {
            throw new InvalidDeliveryException("Sender and recipient cannot be the same user.");
        }
    }
    public Page<Delivery> getInTransitDeliveries(Pageable pageable) {
        return deliveryRepository.findByStatus(DeliveryStatus.IN_TRANSIT, pageable);
    }

}
