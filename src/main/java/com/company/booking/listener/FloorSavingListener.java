package com.company.booking.listener;

import com.company.booking.entity.Floor;
import com.company.booking.service.FloorPlanService;
import com.company.booking.service.FloorPlanValidationException;
import io.jmix.core.EntityStates;
import io.jmix.core.Messages;
import io.jmix.core.event.EntitySavingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Rejects a floor plan that is not PNG or JPEG on every save path, not only the floor form.
 */
@Component
public class FloorSavingListener {

    private final FloorPlanService floorPlanService;
    private final Messages messages;
    private final EntityStates entityStates;

    public FloorSavingListener(FloorPlanService floorPlanService,
                               Messages messages,
                               EntityStates entityStates) {
        this.floorPlanService = floorPlanService;
        this.messages = messages;
        this.entityStates = entityStates;
    }

    @EventListener
    public void onFloorSaving(EntitySavingEvent<Floor> event) {
        Floor floor = event.getEntity();
        // A floor saved without its plan in the fetch plan does not change the plan
        if (entityStates.isLoaded(floor, "plan") && !floorPlanService.isAllowedPlan(floor.getPlan())) {
            throw new FloorPlanValidationException(
                    messages.getMessage("com.company.booking.service", "FloorPlan.wrongType"));
        }
    }
}
