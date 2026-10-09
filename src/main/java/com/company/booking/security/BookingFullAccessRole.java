package com.company.booking.security;

import com.company.booking.entity.Booking;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.model.SecurityScope;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "BookingFullAccess", code = BookingFullAccessRole.CODE, scope = SecurityScope.UI)
public interface BookingFullAccessRole {
    String CODE = "booking-full-access";

    @MenuPolicy(menuIds = "Booking.list")
    @ViewPolicy(viewIds = {
            "Booking.list",
            "Booking.detail"
    })
    void screens();

    @EntityAttributePolicy(entityClass = Booking.class,
            attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Booking.class, actions = EntityPolicyAction.ALL)
    void booking();
}