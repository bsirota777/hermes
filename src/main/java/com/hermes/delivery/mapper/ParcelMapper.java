package com.hermes.delivery.mapper;

import com.hermes.delivery.Parcel;
import com.hermes.delivery.dto.ParcelResponseDto;

public class ParcelMapper {

    public static ParcelResponseDto toDto(Parcel parcel) {
        return new ParcelResponseDto(
                parcel.getId(),
                parcel.getDelivery().getId(),
                parcel.getDescription(),
                parcel.getLengthCm(),
                parcel.getWidthCm(),
                parcel.getHeightCm(),
                parcel.getWeightKg(),
                parcel.getDeclaredValue(),
                parcel.isInsured(),
                parcel.getInsuredValue(),
                parcel.getCreatedAt()
        );
    }
}