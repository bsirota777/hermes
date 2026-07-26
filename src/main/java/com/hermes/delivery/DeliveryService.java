package com.hermes.delivery;

import com.hermes.delivery.exception.InvalidDeliveryException;
import com.hermes.user.RecipientProfile;
import com.hermes.user.SenderProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
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
