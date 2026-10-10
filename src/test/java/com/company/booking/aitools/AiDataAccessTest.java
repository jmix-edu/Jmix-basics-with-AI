package com.company.booking.aitools;

import com.company.booking.entity.*;
import com.company.booking.security.EmployeeRole;
import com.company.booking.security.OfficeManagerRole;
import com.company.booking.security.OwnBookingsRole;
import com.company.booking.security.UiMinimalRole;
import io.jmix.aitools.dataload.execution.JpqlExecutionRequest;
import io.jmix.aitools.dataload.execution.JpqlExecutionResult;
import io.jmix.aitools.dataload.execution.JpqlExecutionService;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data the AI chat can read on behalf of a user (specs/05_addons/01-ai-tools.spec.adoc).
 * Queries go through the add-on's JpqlExecutionService, the same path the model's queries
 * take, so no model and no network are needed.
 */
@SpringBootTest
@ActiveProfiles("test")
public class AiDataAccessTest {

    static final LocalDateTime DAY = LocalDateTime.of(2026, 10, 13, 10, 0);

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    JpqlExecutionService jpqlExecutionService;

    final List<RoleAssignmentEntity> roleAssignments = new ArrayList<>();
    final List<User> users = new ArrayList<>();

    User employee;
    User otherEmployee;
    User manager;
    Building building;
    Booking ownBooking;
    Booking othersBooking;

    @BeforeEach
    void setUp() {
        systemAuthenticator.runWithSystem(() -> {
            String suffix = UUID.randomUUID().toString();
            employee = user("employee-" + suffix,
                    EmployeeRole.CODE, UiMinimalRole.CODE, OwnBookingsRole.CODE);
            otherEmployee = user("other-employee-" + suffix,
                    EmployeeRole.CODE, UiMinimalRole.CODE, OwnBookingsRole.CODE);
            manager = user("manager-" + suffix, OfficeManagerRole.CODE, UiMinimalRole.CODE);

            building = dataManager.create(Building.class);
            building.setName("Здание " + suffix);
            Floor floor = dataManager.create(Floor.class);
            floor.setBuilding(building);
            floor.setNumber(1);
            Room alpha = room(floor, "Альфа");
            Room beta = room(floor, "Бета");
            dataManager.saveWithoutReload(building, floor, alpha, beta);

            ownBooking = dataManager.save(booking(employee, alpha));
            othersBooking = dataManager.save(booking(otherEmployee, beta));
        });
    }

    @AfterEach
    void tearDown() {
        systemAuthenticator.runWithSystem(() -> {
            // Building delete cascades to floors, rooms and their bookings
            dataManager.load(Id.of(building)).optional().ifPresent(dataManager::remove);
            // Role assignments are soft-deletable: remove the rows for good
            SaveContext removeAssignments = new SaveContext().setHint(PersistenceHints.SOFT_DELETION, false);
            roleAssignments.forEach(removeAssignments::removing);
            dataManager.save(removeAssignments);
            users.forEach(user -> dataManager.load(Id.of(user)).optional().ifPresent(dataManager::remove));
        });
    }

    // --- Employee ---

    @Test
    void employeeReadsOnlyOwnBookings() {
        assertThat(ids(employee, "select b.id from Booking b")).containsExactly(ownBooking.getId());
    }

    @Test
    void employeeCountsOnlyOwnBookings() {
        List<Map<String, Object>> rows = query(employee,
                "select count(b) from Booking b where b.room.floor.building.name = '" + building.getName() + "'",
                "bookingCount");

        assertThat(rows).singleElement()
                .satisfies(row -> assertThat(((Number) row.get("bookingCount")).longValue()).isEqualTo(1L));
    }

    @Test
    void employeeReadsOnlyOwnBookingsThroughJoins() {
        assertThat(ids(employee, "select b.id from Room r, Booking b where b.room = r"))
                .containsExactly(ownBooking.getId());
        assertThat(ids(employee, "select b.id from Room r join Booking b on b.room = r"))
                .containsExactly(ownBooking.getId());
    }

    @Test
    void employeeCannotDetectOthersBookingsThroughSubquery() {
        List<Map<String, Object>> rows = query(employee,
                "select r.name from Room r where r.floor.building.name = '" + building.getName() + "'"
                        + " and exists (select b.id from Booking b where b.room = r)",
                "name");

        assertThat(rows).extracting(row -> row.get("name")).containsExactly("Альфа");
    }

    @Test
    void employeeReadsOnlyThemselvesAmongUsers() {
        List<Map<String, Object>> rows = query(employee, "select u.username from User u", "username");

        assertThat(rows).extracting(row -> row.get("username")).containsExactly(employee.getUsername());
    }

    @Test
    void employeeReadsAllRooms() {
        List<Map<String, Object>> rows = query(employee,
                "select r.name from Room r where r.floor.building.name = '" + building.getName() + "'",
                "name");

        assertThat(rows).extracting(row -> row.get("name")).containsExactlyInAnyOrder("Альфа", "Бета");
    }

    // --- Office manager ---

    @Test
    void managerReadsAllBookings() {
        assertThat(ids(manager, "select b.id from Booking b"))
                .contains(ownBooking.getId(), othersBooking.getId());
    }

    @Test
    void managerDoesNotGetUserEmails() {
        List<Map<String, Object>> rows = query(manager,
                "select u.username, u.email from User u where u.username = '" + employee.getUsername() + "'",
                "username", "email");

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("username")).isEqualTo(employee.getUsername());
            assertThat(row).doesNotContainKey("email");
        });
    }

    private List<Object> ids(User user, String jpql) {
        return query(user, jpql, "id").stream().map(row -> row.get("id")).toList();
    }

    private List<Map<String, Object>> query(User user, String jpql, String... resultProperties) {
        JpqlExecutionResult result = systemAuthenticator.withUser(user.getUsername(), () ->
                jpqlExecutionService.execute(new JpqlExecutionRequest(
                        "test", jpql, List.of(), List.of(resultProperties), 100, null)));
        assertThat(result.isExecuted())
                .as("query was not executed: %s", result.getExecutionError())
                .isTrue();
        return result.getRows();
    }

    private User user(String username, String... roleCodes) {
        User user = dataManager.create(User.class);
        user.setUsername(username);
        user.setFirstName("Тест");
        user.setEmail(username + "@example.com");
        user = dataManager.save(user);
        users.add(user);
        for (String roleCode : roleCodes) {
            RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
            assignment.setUsername(username);
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(OwnBookingsRole.CODE.equals(roleCode)
                    ? RoleAssignmentRoleType.ROW_LEVEL
                    : RoleAssignmentRoleType.RESOURCE);
            roleAssignments.add(dataManager.save(assignment));
        }
        return user;
    }

    private Room room(Floor floor, String name) {
        Room room = dataManager.create(Room.class);
        room.setFloor(floor);
        room.setName(name);
        room.setCapacity(4);
        return room;
    }

    private Booking booking(User user, Room room) {
        Booking booking = dataManager.create(Booking.class);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartAt(DAY);
        booking.setEndAt(DAY.plusHours(1));
        return booking;
    }
}
