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
 * Employee from specs/02_security/01-roles.spec.adoc: reads the office directories and
 * manages bookings. Which bookings is narrowed by {@link OwnBookingsRole}.
 */
@ResourceRole(name = "Сотрудник", code = EmployeeRole.CODE)
public interface EmployeeRole {

    String CODE = "employee";

    // No DELETE: a booking is cancelled with status CANCELLED, keeping its history
    @EntityAttributePolicy(entityClass = Booking.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Booking.class,
            actions = {EntityPolicyAction.READ, EntityPolicyAction.CREATE, EntityPolicyAction.UPDATE})
    void booking();

    @EntityAttributePolicy(entityClass = Building.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Building.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Floor.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Floor.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Room.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Room.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Desk.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Desk.class, actions = EntityPolicyAction.READ)
    @EntityAttributePolicy(entityClass = Amenity.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Amenity.class, actions = EntityPolicyAction.READ)
    void directories();

    // Only what the booking author's instance name reads
    @EntityAttributePolicy(entityClass = User.class,
            attributes = {"username", "firstName", "lastName"}, action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();

    // Room.list and Desk.list are lookups from the booking form, not menu items
    @ViewPolicy(viewIds = {"Booking.list", "Booking.detail", "Room.list", "Desk.list"})
    @MenuPolicy(menuIds = "Booking.list")
    void views();
}
