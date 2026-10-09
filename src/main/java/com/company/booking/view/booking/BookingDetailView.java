package com.company.booking.view.booking;

import com.company.booking.entity.Booking;
import com.company.booking.entity.User;
import com.company.booking.service.BookingValidationService;
import com.company.booking.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Route(value = "bookings/:id", layout = MainView.class)
@ViewController(id = "Booking.detail")
@ViewDescriptor(path = "booking-detail-view.xml")
@EditedEntityContainer("bookingDc")
@DialogMode(width = "48em")
public class BookingDetailView extends StandardDetailView<Booking> {

    @Autowired
    private BookingValidationService bookingValidationService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private TimeSource timeSource;

    @Subscribe
    public void onInitEntity(final InitEntityEvent<Booking> event) {
        Booking booking = event.getEntity();
        LocalDateTime nextHour = timeSource.now().toLocalDateTime()
                .truncatedTo(ChronoUnit.HOURS)
                .plusHours(1);
        booking.setStartAt(nextHour);
        booking.setEndAt(nextHour.plusHours(1));
        // The system user has no row in the user table, so it is never set as the author
        if (currentAuthentication.getUser() instanceof User user) {
            booking.setUser(user);
        }
    }

    @Subscribe(id = "bookingDc", target = Target.DATA_CONTAINER)
    public void onBookingDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<Booking> event) {
        // A booking targets either a room or a desk: choosing one clears the other
        if (event.getValue() == null) {
            return;
        }
        if ("room".equals(event.getProperty())) {
            event.getItem().setDesk(null);
        } else if ("desk".equals(event.getProperty())) {
            event.getItem().setRoom(null);
        }
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        bookingValidationService.validate(getEditedEntity())
                .forEach(event.getErrors()::add);
    }
}
