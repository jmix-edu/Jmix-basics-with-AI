package com.company.booking.service;

import com.company.booking.entity.Booking;
import com.company.booking.entity.Floor;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Floor plans from specs/03_files/01-floor-plan.spec.adoc: allowed file types and
 * the floor whose plan a booking shows.
 */
@Service
public class FloorPlanService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private final DataManager dataManager;

    public FloorPlanService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * @return true if there is no plan or the plan is a PNG or JPEG file by its extension
     */
    public boolean isAllowedPlan(@Nullable FileRef plan) {
        if (plan == null) {
            return true;
        }
        String fileName = plan.getFileName();
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && ALLOWED_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /**
     * @return the floor of the booking's room or desk with its plan; empty if no target is chosen
     */
    public Optional<Floor> findFloor(Booking booking) {
        if (booking.getRoom() != null) {
            return dataManager.load(Floor.class)
                    .query("select e from Floor_ e join e.rooms r where r = :room")
                    .parameter("room", booking.getRoom())
                    .optional();
        }
        if (booking.getDesk() != null) {
            return dataManager.load(Floor.class)
                    .query("select e from Floor_ e join e.desks d where d = :desk")
                    .parameter("desk", booking.getDesk())
                    .optional();
        }
        return Optional.empty();
    }
}
