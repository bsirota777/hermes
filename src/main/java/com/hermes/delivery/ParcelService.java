package com.hermes.delivery;

import com.hermes.delivery.dto.CreateParcelRequest;
import com.hermes.delivery.exception.ParcelNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class ParcelService {

    private final ParcelRepository parcelRepository;
    private final DeliveryRepository deliveryRepository;

    public ParcelService(ParcelRepository parcelRepository, DeliveryRepository deliveryRepository) {
        this.parcelRepository = parcelRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public List<Parcel> getParcelsByDeliveryId(Long deliveryId) {
        return parcelRepository.findByDeliveryId(deliveryId);
    }

    public Parcel getParcelById(Long id) {
        return parcelRepository.findById(id)
                .orElseThrow(() -> new ParcelNotFoundException("Parcel not found with id: " + id));
    }

    public Parcel createParcel(CreateParcelRequest request) {
        Delivery delivery = deliveryRepository.findById(request.getDeliveryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Delivery not found with id: " + request.getDeliveryId()));

        Parcel parcel = new Parcel();
        parcel.setDelivery(delivery);
        parcel.setLengthCm(request.getLengthCm());
        parcel.setWidthCm(request.getWidthCm());
        parcel.setHeightCm(request.getHeightCm());
        parcel.setWeightKg(request.getWeightKg());
        parcel.setDeclaredValue(request.getDeclaredValue());
        parcel.setInsured(request.isInsured());
        parcel.setInsuredValue(request.getInsuredValue());

        return parcelRepository.save(parcel);
    }
}