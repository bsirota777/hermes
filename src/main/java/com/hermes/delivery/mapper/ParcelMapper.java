package com.hermes.delivery.mapper;

import com.hermes.delivery.Parcel;
import com.hermes.delivery.dto.ParcelDto;

public class ParcelMapper {

    public static ParcelDto toDto(Parcel parcel) {
        ParcelDto dto = new ParcelDto();
        dto.setId(parcel.getId());
        dto.setDeliveryId(parcel.getDelivery().getId());
        dto.setLengthCm(parcel.getLengthCm());
        dto.setWidthCm(parcel.getWidthCm());
        dto.setHeightCm(parcel.getHeightCm());
        dto.setWeightKg(parcel.getWeightKg());
        dto.setDeclaredValue(parcel.getDeclaredValue());
        dto.setInsured(parcel.isInsured());
        dto.setInsuredValue(parcel.getInsuredValue());
        dto.setCreatedAt(parcel.getCreatedAt());
        return dto;
    }
}