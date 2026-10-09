package com.company.booking.service;

import com.company.booking.entity.Booking;
import com.company.booking.entity.BookingStatus;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Room;
import io.jmix.core.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Booking rules from specs/01_foundations/03-slot-conflict-service.spec.adoc:
 * R1 end after start, R2 exactly one target, R3 the target is free for [startAt, endAt).
 */
@Service
public class BookingValidationService {

    private static final String MESSAGE_GROUP = "com.company.booking.service";

    private final UnconstrainedDataManager unconstrainedDataManager;
    private final Messages messages;
    private final EntityStates entityStates;
    private final MetadataTools metadataTools;

    public BookingValidationService(UnconstrainedDataManager unconstrainedDataManager,
                                    Messages messages,
                                    EntityStates entityStates,
                                    MetadataTools metadataTools) {
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.messages = messages;
        this.entityStates = entityStates;
        this.metadataTools = metadataTools;
    }

    /**
     * Checks the booking against R1-R3 without saving anything.
     *
     * @return localized violation messages; empty if the booking is valid
     */
    public List<String> validate(Booking booking) {
        List<String> errors = new ArrayList<>();

        LocalDateTime startAt = booking.getStartAt();
        LocalDateTime endAt = booking.getEndAt();
        // Empty start/end is reported by @NotNull; an inverted interval makes R3 meaningless
        boolean intervalValid = startAt != null && endAt != null && endAt.isAfter(startAt);
        if (startAt != null && endAt != null && !intervalValid) {
            errors.add(message("BookingValidation.endNotAfterStart"));
        }

        Room room = reference(booking, "room", Booking::getRoom);
        Desk desk = reference(booking, "desk", Booking::getDesk);
        if (room == null && desk == null) {
            errors.add(message("BookingValidation.noTarget"));
        } else if (room != null && desk != null) {
            errors.add(message("BookingValidation.bothTargets"));
        } else if (intervalValid && booking.getStatus() != BookingStatus.CANCELLED) {
            Object target = room != null ? room : desk;
            String targetProperty = room != null ? "room" : "desk";
            String busyKey = room != null ? "BookingValidation.roomBusy" : "BookingValidation.deskBusy";
            findConflict(booking, targetProperty, target).ifPresent(conflict ->
                    errors.add(messages.formatMessage(MESSAGE_GROUP, busyKey,
                            metadataTools.getInstanceName(target),
                            metadataTools.getInstanceName(conflict))));
        }
        return errors;
    }

    /**
     * Searches all bookings, not only those the current user may read: a row-level role
     * such as "own bookings only" must not hide a conflicting booking of another user.
     */
    private Optional<Booking> findConflict(Booking booking, String targetProperty, Object target) {
        return unconstrainedDataManager.load(Booking.class)
                .query("select e from Booking e where e." + targetProperty + " = :target"
                        + " and e.status <> :cancelled and e.id <> :id"
                        + " and e.startAt < :endAt and e.endAt > :startAt"
                        + " order by e.startAt")
                .parameter("target", target)
                .parameter("cancelled", BookingStatus.CANCELLED.getId())
                .parameter("id", booking.getId())
                .parameter("startAt", booking.getStartAt())
                .parameter("endAt", booking.getEndAt())
                .maxResults(1)
                .optional();
    }

    /**
     * Reads a reference that may be unfetched: inside EntitySavingEvent an unfetched
     * reference is not lazy-loaded, so it is read from a separately loaded copy.
     */
    private <T> T reference(Booking booking, String property, Function<Booking, T> getter) {
        if (entityStates.isLoaded(booking, property)) {
            return getter.apply(booking);
        }
        return unconstrainedDataManager.load(Booking.class)
                .id(booking.getId())
                .fetchPlan(fp -> fp.add(property, FetchPlan.INSTANCE_NAME))
                .optional()
                .map(getter)
                .orElse(null);
    }

    private String message(String key) {
        return messages.getMessage(MESSAGE_GROUP, key);
    }
}
