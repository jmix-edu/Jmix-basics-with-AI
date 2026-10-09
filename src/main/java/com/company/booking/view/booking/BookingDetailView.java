package com.company.booking.view.booking;

import com.company.booking.entity.Booking;
import com.company.booking.entity.Floor;
import com.company.booking.entity.User;
import com.company.booking.service.BookingValidationService;
import com.company.booking.service.FloorPlanService;
import com.company.booking.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Route(value = "bookings/:id", layout = MainView.class)
@ViewController(id = "Booking.detail")
@ViewDescriptor(path = "booking-detail-view.xml")
@EditedEntityContainer("bookingDc")
@DialogMode(width = "64em")
public class BookingDetailView extends StandardDetailView<Booking> {

    @Autowired
    private BookingValidationService bookingValidationService;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private FloorPlanService floorPlanService;
    @Autowired
    private Downloader downloader;

    @ViewComponent
    private InstanceContainer<Floor> floorDc;
    @ViewComponent
    private VerticalLayout planBox;
    @ViewComponent
    private JmixImage<Object> planImage;
    @ViewComponent
    private Span noPlanText;
    @ViewComponent
    private JmixButton downloadPlanButton;

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

    @Subscribe
    public void onReady(final ReadyEvent event) {
        updatePlan();
    }

    @Subscribe(id = "bookingDc", target = Target.DATA_CONTAINER)
    public void onBookingDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<Booking> event) {
        if (!"room".equals(event.getProperty()) && !"desk".equals(event.getProperty())) {
            return;
        }
        // A booking targets either a room or a desk: choosing one clears the other
        if (event.getValue() != null) {
            if ("room".equals(event.getProperty())) {
                event.getItem().setDesk(null);
            } else {
                event.getItem().setRoom(null);
            }
        }
        updatePlan();
    }

    @Subscribe(id = "downloadPlanButton", subject = "clickListener")
    public void onDownloadPlanButtonClick(final ClickEvent<JmixButton> event) {
        Floor floor = floorDc.getItemOrNull();
        if (floor != null && floor.getPlan() != null) {
            // By default images open in a browser tab; the button must save the file.
            // Downloader is a prototype bean, so this affects only this view.
            downloader.setViewFilePredicate(extension -> false);
            downloader.download(floor.getPlan());
        }
    }

    private void updatePlan() {
        Floor floor = floorPlanService.findFloor(getEditedEntity()).orElse(null);
        floorDc.setItem(floor);
        boolean hasPlan = floor != null && floor.getPlan() != null;
        planBox.setVisible(floor != null);
        planImage.setVisible(hasPlan);
        noPlanText.setVisible(!hasPlan);
        downloadPlanButton.setEnabled(hasPlan);
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        bookingValidationService.validate(getEditedEntity())
                .forEach(event.getErrors()::add);
    }
}
