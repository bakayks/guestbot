package com.guestbot.service.booking;

import com.guestbot.core.entity.Booking;
import com.guestbot.core.entity.Room;
import com.guestbot.core.enums.BookingStatus;
import com.guestbot.core.enums.CancellationReason;
import com.guestbot.core.event.DomainEventPublisher;
import com.guestbot.core.event.DomainEvents.*;
import com.guestbot.core.exception.GuestBotException;
import com.guestbot.core.exception.ResourceNotFoundException;
import com.guestbot.repository.BookingRepository;
import com.guestbot.repository.HotelRepository;
import com.guestbot.repository.RoomRepository;
import com.guestbot.service.calendar.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final CalendarService calendarService;
    private final DomainEventPublisher eventPublisher;


    @Transactional
    public Booking create(
        Long hotelId,
        Long roomId,
        String guestName,
        String guestPhone,
        String guestEmail,
        LocalDate checkIn,
        LocalDate checkOut,
        Long telegramChatId
    ) {
        // Pessimistic lock + availability check
        calendarService.validateAndLockRoom(roomId, checkIn, checkOut);

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        int nights = (int) checkIn.until(checkOut).getDays();

        Booking booking = new Booking();
        booking.setBookingNumber(generateBookingNumber());
        booking.setHotel(room.getHotel());
        booking.setRoom(room);
        booking.setGuestName(guestName);
        booking.setGuestPhone(guestPhone);
        booking.setGuestEmail(guestEmail);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setNights(nights);
        booking.setTotalAmount(room.getPricePerNight().multiply(
            java.math.BigDecimal.valueOf(nights)
        ));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentDeadline(LocalDateTime.now().plusHours(24));
        booking.setTelegramChatId(telegramChatId);

        booking = bookingRepository.save(booking);

        eventPublisher.publish(new BookingCreatedEvent(
            booking.getId(),
            booking.getHotel().getId(),
            booking.getRoom().getId(),
            booking.getGuestName(),
            booking.getGuestPhone(),
            booking.getGuestEmail(),
            booking.getCheckIn(),
            booking.getCheckOut(),
            booking.getNights(),
            booking.getTotalAmount()
        ));

        return booking;
    }

    @Transactional
    public Booking confirm(Long bookingId, String transactionId) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new GuestBotException("Booking " + bookingId + " is not in PENDING_PAYMENT status");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancel(Long bookingId, CancellationReason reason) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new GuestBotException("Cannot cancel completed booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        eventPublisher.publish(new BookingCancelledEvent(
            booking.getId(),
            booking.getHotel().getId(),
            booking.getGuestPhone(),
            booking.getGuestName(),
            reason
        ));

        return booking;
    }

    // Запускается @Scheduled каждые 30 минут
    @Transactional
    public void cancelExpiredBookings() {
        List<Booking> expired = bookingRepository.findByStatusAndPaymentDeadlineBefore(
            BookingStatus.PENDING_PAYMENT,
            LocalDateTime.now()
        );

        log.info("Auto-cancelling {} expired bookings", expired.size());

        expired.forEach(b -> {
            b.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(b);
            eventPublisher.publish(new BookingCancelledEvent(
                b.getId(), b.getHotel().getId(),
                b.getGuestPhone(), b.getGuestName(),
                CancellationReason.PAYMENT_TIMEOUT
            ));
        });
    }

    // Запускается @Scheduled каждый день в 10:00
    @Transactional(readOnly = true)
    public void sendCheckInReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Booking> checkIns = bookingRepository.findConfirmedCheckInsForDate(tomorrow);

        checkIns.forEach(b -> eventPublisher.publish(new GuestCheckInTomorrowEvent(
            b.getId(), b.getHotel().getId(),
            b.getGuestPhone(), b.getGuestName(),
            b.getCheckIn()
        )));
    }

    @Transactional(readOnly = true)
    public Booking getById(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    @Transactional(readOnly = true)
    public Booking getByNumber(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber)
            .orElseThrow(() -> new GuestBotException("Booking not found: " + bookingNumber));
    }

    private String generateBookingNumber() {
        LocalDate today = LocalDate.now();
        String prefix = "BK-" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        long count = bookingRepository.countByBookingNumberPrefix(prefix);
        return prefix + String.format("%03d", count + 1);
    }
}
