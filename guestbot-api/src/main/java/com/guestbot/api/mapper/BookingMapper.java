package com.guestbot.api.mapper;

import com.guestbot.api.dto.response.BookingResponse;
import com.guestbot.api.dto.response.RoomAvailabilityResponse;
import com.guestbot.core.entity.Booking;
import com.guestbot.service.calendar.CalendarService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoomMapper.class})
public interface BookingMapper {

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "roomType", source = "room.type")
    BookingResponse toResponse(Booking booking);

    List<BookingResponse> toList(List<Booking> bookings);

    @Mapping(target = "room", source = "room")
    @Mapping(target = "nights", expression = "java(availability.nights())")
    RoomAvailabilityResponse toAvailabilityResponse(CalendarService.RoomAvailability availability);

    List<RoomAvailabilityResponse> toAvailabilityList(List<CalendarService.RoomAvailability> availabilities);
}
