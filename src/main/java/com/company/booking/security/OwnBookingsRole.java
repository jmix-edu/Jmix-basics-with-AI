package com.company.booking.security;

import com.company.booking.entity.Booking;
import com.company.booking.entity.User;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.security.model.RowLevelBiPredicate;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.JpqlRowLevelPolicy;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;
import org.springframework.context.ApplicationContext;

/**
 * Narrows the employee to own bookings and to their own user record
 * (specs/02_security/01-roles.spec.adoc).
 */
@RowLevelRole(name = "Только свои брони", code = OwnBookingsRole.CODE)
public interface OwnBookingsRole {

    String CODE = "own-bookings";

    @JpqlRowLevelPolicy(entityClass = Booking.class,
            where = "{E}.user.id = :current_user_id")
    void readOwnBookings();

    @JpqlRowLevelPolicy(entityClass = User.class, where = "{E}.id = :current_user_id")
    void readOwnUser();
}
