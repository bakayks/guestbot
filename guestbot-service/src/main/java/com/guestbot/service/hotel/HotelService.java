package com.guestbot.service.hotel;

import com.guestbot.core.entity.Hotel;
import com.guestbot.core.entity.Owner;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.repository.HotelRepository;
import com.guestbot.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final OwnerRepository ownerRepository;

    @Transactional(readOnly = true)
    public List<Hotel> getByOwner(Long ownerId) {
        return hotelRepository.findByOwnerIdAndActiveTrue(ownerId);
    }

    @Transactional(readOnly = true)
    public Hotel getById(Long id) {
        return hotelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Hotel", id));
    }

    @Transactional(readOnly = true)
    public List<Hotel> getActiveBotHotels() {
        return hotelRepository.findByBotActiveTrueAndActiveTrue();
    }

    @Transactional
    public Hotel create(Long ownerId, Hotel hotel) {
        Owner owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", ownerId));
        hotel.setOwner(owner);
        return hotelRepository.save(hotel);
    }

    @Transactional
    public Hotel update(Long id, Long ownerId, Hotel updated) {
        Hotel hotel = getById(id);
        validateOwnership(hotel, ownerId);

        hotel.setName(updated.getName());
        hotel.setDescription(updated.getDescription());
        hotel.setAddress(updated.getAddress());
        hotel.setCity(updated.getCity());
        hotel.setPhone(updated.getPhone());
        hotel.setEmail(updated.getEmail());
        hotel.setWebsite(updated.getWebsite());
        hotel.setLatitude(updated.getLatitude());
        hotel.setLongitude(updated.getLongitude());
        hotel.setAmenities(updated.getAmenities());
        hotel.setCheckInTime(updated.getCheckInTime());
        hotel.setCheckOutTime(updated.getCheckOutTime());
        hotel.setMinAge(updated.getMinAge());
        hotel.setPetsAllowed(updated.getPetsAllowed());
        hotel.setChildrenAllowed(updated.getChildrenAllowed());
        hotel.setCancellationPolicy(updated.getCancellationPolicy());
        hotel.setWelcomeMessage(updated.getWelcomeMessage());
        hotel.setOffHoursMessage(updated.getOffHoursMessage());
        hotel.setWorkingHoursStart(updated.getWorkingHoursStart());
        hotel.setWorkingHoursEnd(updated.getWorkingHoursEnd());

        return hotelRepository.save(hotel);
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        Hotel hotel = getById(id);
        validateOwnership(hotel, ownerId);
        hotel.setActive(false);
        hotelRepository.save(hotel);
    }

    private void validateOwnership(Hotel hotel, Long ownerId) {
        if (!hotel.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException("Hotel", hotel.getId());
        }
    }
}
