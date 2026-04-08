package com.guestbot.api.mapper;

import com.guestbot.api.dto.request.RoomRequest;
import com.guestbot.api.dto.response.RoomResponse;
import com.guestbot.core.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    RoomResponse toResponse(Room room);

    List<RoomResponse> toList(List<Room> rooms);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "active", ignore = true)
    Room toEntity(RoomRequest request);
}
