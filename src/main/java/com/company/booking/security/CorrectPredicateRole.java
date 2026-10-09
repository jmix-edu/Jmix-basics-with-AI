package com.company.booking.security;

import com.company.booking.entity.Booking;
import com.company.booking.entity.User;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.security.model.RowLevelBiPredicate;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;
import org.springframework.context.ApplicationContext;

@RowLevelRole(name = "CorrectPredicateRole", code = CorrectPredicateRole.CODE)
public interface CorrectPredicateRole {
    String CODE = "correct-predicate-role";

    // JPQL policies do not apply to saving: without this an employee could book on behalf of others
    @PredicateRowLevelPolicy(entityClass = Booking.class,
            actions = {RowLevelPolicyAction.CREATE, RowLevelPolicyAction.UPDATE, RowLevelPolicyAction.DELETE})
    default RowLevelBiPredicate<Booking, ApplicationContext> writeOwnBookings() {
        return (booking, applicationContext) -> {
            Object currentUser = applicationContext.getBean(CurrentAuthentication.class).getUser();
            return booking.getUser() != null
                    && currentUser instanceof User user
                    && user.getId().equals(booking.getUser().getId());
        };
    }
}