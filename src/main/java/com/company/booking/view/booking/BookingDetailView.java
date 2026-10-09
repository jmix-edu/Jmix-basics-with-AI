package com.company.booking.view.booking;

import com.company.booking.entity.Booking;
import com.company.booking.service.BookingValidationService;
import com.company.booking.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;


@Route(value = "bookings/:id", layout = MainView.class)
@ViewController(id = "Booking.detail")
@ViewDescriptor(path = "booking-detail-view.xml")
@EditedEntityContainer("bookingDc")
public class BookingDetailView extends StandardDetailView<Booking> {

    @Autowired
    private BookingValidationService bookingValidationService;

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        bookingValidationService.validate(getEditedEntity())
                .forEach(event.getErrors()::add);
    }
}
