package com.company.booking.security;

import com.company.booking.entity.User;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.model.SecurityScope;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "UserReadOnly", code = UserReadOnlyRole.CODE, scope = SecurityScope.UI)
public interface UserReadOnlyRole {
    String CODE = "user-read-only";

    @MenuPolicy(menuIds = "User.list")
    @ViewPolicy(viewIds = {
            "User.list",
            "User.detail"
    })
    void screens();

    @EntityAttributePolicy(entityClass = User.class,
            attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();
}