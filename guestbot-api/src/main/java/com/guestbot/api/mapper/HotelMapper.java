package com.guestbot.api.mapper;

import com.guestbot.api.dto.request.HotelRequest;
import com.guestbot.api.dto.response.HotelResponse;
import com.guestbot.core.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    HotelResponse toResponse(Hotel hotel);

    List<HotelResponse> toList(List<Hotel> hotels);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "active", ignore = true)
    Hotel toEntity(HotelRequest request);
}
