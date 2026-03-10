package com.guestbot.service.room;

import com.guestbot.core.entity.Hotel;
import com.guestbot.core.entity.Room;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.repository.HotelRepository;
import com.guestbot.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    @Transactional(readOnly = true)
    public List<Room> getByHotel(Long hotelId) {
        return roomRepository.findByHotelIdAndActiveTrue(hotelId);
    }

    @Transactional(readOnly = true)
    public Room getById(Long id) {
        return roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    @Transactional
    public Room create(Long hotelId, Room room) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", hotelId));
        room.setHotel(hotel);
        return roomRepository.save(room);
    }

    @Transactional
    public Room update(Long id, Room updated) {
        Room room = getById(id);
        room.setType(updated.getType());
        room.setDescription(updated.getDescription());
        room.setCapacity(updated.getCapacity());
        room.setCount(updated.getCount());
        room.setPricePerNight(updated.getPricePerNight());
        room.setAmenities(updated.getAmenities());
        return roomRepository.save(room);
    }

    @Transactional
    public void delete(Long id) {
        Room room = getById(id);
        room.setActive(false);
        roomRepository.save(room);
    }
}
