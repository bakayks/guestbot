package com.guestbot.service.calendar;

import com.guestbot.core.entity.AvailabilityBlock;
import com.guestbot.core.entity.Room;
import com.guestbot.core.exception.RoomNotAvailableException;
import com.guestbot.repository.AvailabilityBlockRepository;
import com.guestbot.repository.BookingRepository;
import com.guestbot.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityBlockRepository availabilityBlockRepository;

    /**
     * Проверяет доступность всех номеров гостиницы на диапазон дат.
     * Возвращает список доступных номеров.
     */
    @Transactional(readOnly = true)
    public List<RoomAvailability> checkAvailability(
        Long hotelId,
        LocalDate checkIn,
        LocalDate checkOut
    ) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }

        List<Room> rooms = roomRepository.findByHotelIdAndActiveTrue(hotelId);
        List<RoomAvailability> result = new ArrayList<>();

        for (Room room : rooms) {
            long bookedCount = bookingRepository.countActiveBookingsForRoom(
                room.getId(), checkIn, checkOut
            );
            long blockedCount = availabilityBlockRepository.countBlocksForRoom(
                room.getId(), checkIn, checkOut
            );
            long available = room.getCount() - bookedCount - blockedCount;

            if (available > 0) {
                room.getPhotos().size(); // eager-init within transaction
                result.add(new RoomAvailability(room, available, checkIn, checkOut));
            }
        }

        return result;
    }

    /**
     * Проверяет доступность конкретного номера.
     * Использует PESSIMISTIC_WRITE lock для предотвращения двойного бронирования.
     */
    @Transactional
    public void validateAndLockRoom(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        // Pessimistic lock на уровне записи room
        Room room = roomRepository.findByIdWithLock(roomId)
            .orElseThrow(() -> new RoomNotAvailableException(roomId));

        long booked = bookingRepository.countActiveBookingsForRoom(
            roomId, checkIn, checkOut
        );
        long blocked = availabilityBlockRepository.countBlocksForRoom(
            roomId, checkIn, checkOut
        );

        if (booked + blocked >= room.getCount()) {
            throw new RoomNotAvailableException(roomId);
        }
    }

    @Transactional
    public AvailabilityBlock blockDates(
        Long hotelId,
        Long roomId,
        LocalDate dateFrom,
        LocalDate dateTo,
        String reason
    ) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RoomNotAvailableException(roomId));

        AvailabilityBlock block = new AvailabilityBlock();
        block.setRoom(room);
        block.setHotel(room.getHotel());
        block.setDateFrom(dateFrom);
        block.setDateTo(dateTo);
        block.setReason(reason);

        return availabilityBlockRepository.save(block);
    }

    @Transactional
    public void unblockDates(Long blockId) {
        availabilityBlockRepository.deleteById(blockId);
    }

    public record RoomAvailability(
        Room room,
        long availableCount,
        LocalDate checkIn,
        LocalDate checkOut
    ) {
        public int nights() {
            return (int) checkIn.until(checkOut).getDays();
        }
    }
}
