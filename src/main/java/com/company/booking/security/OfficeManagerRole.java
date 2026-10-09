package com.company.booking.security;

import com.company.booking.entity.*;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Office manager from specs/02_security/01-roles.spec.adoc: manages the office space
 * and reads all bookings.
 */
@ResourceRole(name = "Офис-менеджер", code = OfficeManagerRole.CODE)
public interface OfficeManagerRole {

    String CODE = "office-manager";

    @EntityAttributePolicy(entityClass = Building.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Building.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Floor.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Floor.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Room.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Room.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Desk.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Desk.class, actions = EntityPolicyAction.ALL)
    @EntityAttributePolicy(entityClass = Amenity.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Amenity.class, actions = EntityPolicyAction.ALL)
    void directories();

    @EntityAttributePolicy(entityClass = Booking.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Booking.class, actions = EntityPolicyAction.READ)
    void booking();

    // Only what the booking author's instance name reads
    @EntityAttributePolicy(entityClass = User.class,
            attributes = {"username", "firstName", "lastName"}, action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();

    // Floor, room and desk details are dialogs opened from the building and floor forms
    @ViewPolicy(viewIds = {
            "Building.list", "Building.detail", "Floor_.detail", "Room.detail", "Desk.detail",
            "Amenity.list", "Amenity.detail",
            "Booking.list", "Booking.detail"
    })
    @MenuPolicy(menuIds = {"Booking.list", "Building.list", "Amenity.list"})
    void views();
}
