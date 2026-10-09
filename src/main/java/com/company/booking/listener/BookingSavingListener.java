package com.company.booking.listener;

import com.company.booking.entity.Booking;
import com.company.booking.service.BookingValidationException;
import com.company.booking.service.BookingValidationService;
import io.jmix.core.event.EntitySavingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rejects any save of an invalid booking, whatever the save path (view, service, REST, test).
 */
@Component
public class BookingSavingListener {

    private final BookingValidationService bookingValidationService;

    public BookingSavingListener(BookingValidationService bookingValidationService) {
        this.bookingValidationService = bookingValidationService;
    }

    @EventListener
    public void onBookingSaving(EntitySavingEvent<Booking> event) {
        List<String> errors = bookingValidationService.validate(event.getEntity());
        if (!errors.isEmpty()) {
            throw new BookingValidationException(errors);
        }
    }
}
