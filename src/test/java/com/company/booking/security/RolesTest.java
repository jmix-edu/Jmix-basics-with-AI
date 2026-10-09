package com.company.booking.security;

import com.company.booking.entity.*;
import com.company.booking.service.BookingValidationException;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.AccessDeniedException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roles from specs/02_security/01-roles.spec.adoc, checked through DataManager
 * under real users with assigned roles.
 */
@SpringBootTest
@ActiveProfiles("test")
public class RolesTest {

    static final LocalDateTime DAY = LocalDateTime.of(2026, 10, 12, 0, 0);

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    final List<RoleAssignmentEntity> roleAssignments = new ArrayList<>();
    final List<User> users = new ArrayList<>();
    // Buildings and amenities to remove; a building takes its floors, rooms, desks and bookings
    final List<Object> directories = new ArrayList<>();

    User employee;
    User otherEmployee;
    User manager;
    Building building;
    Floor floor;
    Room room;
    Room otherRoom;
    Desk desk;
    Amenity amenity;

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
            floor = dataManager.create(Floor.class);
            floor.setBuilding(building);
            floor.setNumber(1);
            room = dataManager.create(Room.class);
            room.setFloor(floor);
            room.setName("Альфа");
            room.setCapacity(6);
            otherRoom = dataManager.create(Room.class);
            otherRoom.setFloor(floor);
            otherRoom.setName("Бета");
            otherRoom.setCapacity(4);
            desk = dataManager.create(Desk.class);
            desk.setFloor(floor);
            desk.setCode("A-1");
            amenity = dataManager.create(Amenity.class);
            amenity.setName("Проектор " + suffix);
            dataManager.saveWithoutReload(building, floor, room, otherRoom, desk, amenity);
            directories.add(building);
            directories.add(amenity);
        });
    }

    @AfterEach
    void tearDown() {
        systemAuthenticator.runWithSystem(() -> {
            directories.forEach(entity ->
                    dataManager.load(Id.of(entity)).optional().ifPresent(dataManager::remove));
            // Role assignments are soft-deletable: remove the rows for good
            SaveContext removeAssignments = new SaveContext().setHint(PersistenceHints.SOFT_DELETION, false);
            roleAssignments.forEach(removeAssignments::removing);
            dataManager.save(removeAssignments);
            users.forEach(user -> dataManager.load(Id.of(user)).optional().ifPresent(dataManager::remove));
        });
    }

    // --- Employee ---

    @Test
    void employeeSeesOnlyOwnBookings() {
        Booking own = asSystem(booking(employee, room));
        asSystem(booking(otherEmployee, otherRoom));

        List<Booking> visible = systemAuthenticator.withUser(employee.getUsername(),
                () -> dataManager.load(Booking.class).all().list());

        assertThat(visible).extracting(Booking::getId).containsExactly(own.getId());
    }

    @Test
    void employeeCreatesOwnBooking() {
        Booking saved = systemAuthenticator.withUser(employee.getUsername(),
                () -> dataManager.save(booking(employee, room)));

        assertThat(asSystemLoad(saved)).isNotNull();
    }

    @Test
    void employeeCannotCreateBookingForAnotherUser() {
        Booking forOther = booking(otherEmployee, room);

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(employee.getUsername(),
                () -> dataManager.save(forOther)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(asSystemLoad(forOther)).isNull();
    }

    @Test
    void employeeCannotReassignOwnBooking() {
        Booking own = asSystem(booking(employee, room));

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(employee.getUsername(), () -> {
            Booking loaded = dataManager.load(Id.of(own)).one();
            loaded.setUser(otherEmployee);
            dataManager.save(loaded);
        })).isInstanceOf(AccessDeniedException.class);
        assertThat(asSystemLoad(own).getUser()).isEqualTo(employee);
    }

    @Test
    void employeeCannotFindOrChangeOthersBooking() {
        Booking others = asSystem(booking(otherEmployee, room));

        boolean found = systemAuthenticator.withUser(employee.getUsername(),
                () -> dataManager.load(Id.of(others)).optional().isPresent());
        assertThat(found).isFalse();

        others.setComment("Чужая правка");
        assertThatThrownBy(() -> systemAuthenticator.runWithUser(employee.getUsername(),
                () -> dataManager.save(others)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(asSystemLoad(others).getComment()).isNull();
    }

    @Test
    void employeeCannotDeleteOwnBooking() {
        Booking own = asSystem(booking(employee, room));

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(employee.getUsername(),
                () -> dataManager.remove(dataManager.load(Id.of(own)).one())))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(asSystemLoad(own)).isNotNull();
    }

    @Test
    void employeeCannotCreateDirectories() {
        assertThatThrownBy(() -> systemAuthenticator.runWithUser(employee.getUsername(), () -> {
            Building newBuilding = dataManager.create(Building.class);
            newBuilding.setName("Менеджер " + UUID.randomUUID());
            dataManager.save(newBuilding);
        })).isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(employee.getUsername(), () -> {
            Amenity newAmenity = dataManager.create(Amenity.class);
            newAmenity.setName("Менеджер " + UUID.randomUUID());
            dataManager.save(newAmenity);
        })).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void employeeReadsDirectories() {
        systemAuthenticator.runWithUser(employee.getUsername(), () -> {
            assertThat(dataManager.load(Id.of(building)).optional()).isPresent();
            assertThat(dataManager.load(Id.of(floor)).optional()).isPresent();
            assertThat(dataManager.load(Id.of(room)).optional()).isPresent();
            assertThat(dataManager.load(Id.of(desk)).optional()).isPresent();
            assertThat(dataManager.load(Id.of(amenity)).optional()).isPresent();
        });
    }

    @Test
    void employeeCannotBookSlotBusyByOthers() {
        asSystem(booking(otherEmployee, room));

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(employee.getUsername(),
                () -> dataManager.save(booking(employee, room))))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Переговорка «Альфа» уже забронирована на это время: 12.10.2026 10:00 – 11:00.");
    }

    // --- Office manager ---

    @Test
    void managerSeesAllBookings() {
        Booking first = asSystem(booking(employee, room));
        Booking second = asSystem(booking(otherEmployee, otherRoom));

        List<Booking> visible = systemAuthenticator.withUser(manager.getUsername(),
                () -> dataManager.load(Booking.class)
                        .query("select e from Booking e where e.room.floor = :floor")
                        .parameter("floor", floor)
                        .list());

        assertThat(visible).extracting(Booking::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void managerCannotCreateUpdateOrDeleteBookings() {
        Booking existing = asSystem(booking(employee, room));

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(manager.getUsername(),
                () -> dataManager.save(booking(manager, otherRoom))))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(manager.getUsername(), () -> {
            Booking loaded = dataManager.load(Id.of(existing)).one();
            loaded.setComment("Правка менеджера");
            dataManager.save(loaded);
        })).isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> systemAuthenticator.runWithUser(manager.getUsername(),
                () -> dataManager.remove(dataManager.load(Id.of(existing)).one())))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(asSystemLoad(existing).getComment()).isNull();
    }

    @Test
    void managerManagesDirectories() {
        systemAuthenticator.runWithUser(manager.getUsername(), () -> {
            Building newBuilding = dataManager.create(Building.class);
            newBuilding.setName("Менеджер " + UUID.randomUUID());
            Floor newFloor = dataManager.create(Floor.class);
            newFloor.setBuilding(newBuilding);
            newFloor.setNumber(1);
            Room newRoom = dataManager.create(Room.class);
            newRoom.setFloor(newFloor);
            newRoom.setName("Гамма");
            newRoom.setCapacity(10);
            Desk newDesk = dataManager.create(Desk.class);
            newDesk.setFloor(newFloor);
            newDesk.setCode("B-1");
            Amenity newAmenity = dataManager.create(Amenity.class);
            newAmenity.setName("Менеджер " + UUID.randomUUID());
            directories.add(newBuilding);
            directories.add(newAmenity);
            dataManager.saveWithoutReload(newBuilding, newFloor, newRoom, newDesk, newAmenity);

            Room loadedRoom = dataManager.load(Id.of(newRoom)).one();
            loadedRoom.setCapacity(12);
            dataManager.saveWithoutReload(loadedRoom);
            assertThat(dataManager.load(Id.of(newRoom)).one().getCapacity()).isEqualTo(12);

            dataManager.remove(dataManager.load(Id.of(newAmenity)).one());
            dataManager.remove(dataManager.load(Id.of(newBuilding)).one());
            assertThat(dataManager.load(Id.of(newBuilding)).optional()).isEmpty();
            assertThat(dataManager.load(Id.of(newRoom)).optional()).isEmpty();
        });
    }

    private User user(String username, String... roleCodes) {
        User user = dataManager.create(User.class);
        user.setUsername(username);
        user.setFirstName("Тест");
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

    /** A 10:00–11:00 booking; slot conflicts between users are part of the checks. */
    private Booking booking(User user, Room room) {
        Booking booking = dataManager.create(Booking.class);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartAt(DAY.withHour(10));
        booking.setEndAt(DAY.withHour(11));
        return booking;
    }

    private Booking asSystem(Booking booking) {
        return systemAuthenticator.withSystem(() -> dataManager.save(booking));
    }

    private Booking asSystemLoad(Booking booking) {
        return systemAuthenticator.withSystem(() -> dataManager.load(Booking.class)
                .id(booking.getId())
                .fetchPlan(fp -> fp.addFetchPlan("_base").add("user", "_instance_name"))
                .optional()
                .orElse(null));
    }
}
