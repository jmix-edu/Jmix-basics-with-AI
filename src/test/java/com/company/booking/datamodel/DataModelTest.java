package com.company.booking.datamodel;

import com.company.booking.entity.*;
import com.company.booking.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Data model rules from specs/01_foundations/01-data-model.spec.adoc:
 * booking status default and delete behavior of the building aggregate.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@ActiveProfiles("test")
public class DataModelTest {

    @Autowired
    DataManager dataManager;

    // Removed in this order: building (cascades to floors, rooms, desks, bookings), then user, then amenity.
    // Room amenities are unlinked first: ROOM_AMENITY_LINK has no ON DELETE CASCADE yet (spec backlog).
    final List<Object> cleanup = new ArrayList<>();

    Building building;
    Floor floor;
    Room room;
    Desk desk;
    Amenity amenity;
    User user;

    @Test
    void bookingStatusDefaultsToConfirmed() {
        givenBuilding();

        Booking booking = dataManager.create(Booking.class);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        booking.setEndAt(LocalDateTime.of(2026, 10, 8, 11, 0));
        dataManager.save(booking);

        Booking loaded = dataManager.load(Booking.class).id(booking.getId()).one();
        assertThat(loaded.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void deletingBuildingDeletesItsAggregateAndBookings() {
        givenBuilding();
        Booking roomBooking = givenBooking(room, null);
        Booking deskBooking = givenBooking(null, desk);

        dataManager.remove(dataManager.load(Id.of(building)).one());

        assertThat(dataManager.load(Id.of(floor)).optional()).isEmpty();
        assertThat(dataManager.load(Id.of(room)).optional()).isEmpty();
        assertThat(dataManager.load(Id.of(desk)).optional()).isEmpty();
        assertThat(dataManager.load(Id.of(roomBooking)).optional()).isEmpty();
        assertThat(dataManager.load(Id.of(deskBooking)).optional()).isEmpty();
    }

    @Test
    void amenityAssignedToRoomCannotBeDeleted() {
        givenBuilding();
        givenRoomAmenity();

        Amenity assigned = dataManager.load(Id.of(amenity)).one();
        assertThatThrownBy(() -> dataManager.remove(assigned));

        assertThat(dataManager.load(Id.of(amenity)).optional()).isPresent();
    }

    private void givenBuilding() {
        String suffix = UUID.randomUUID().toString();

        user = dataManager.create(User.class);
        user.setUsername("data-model-" + suffix);
        user.setFirstName("Тест");
        user = dataManager.save(user);

        building = dataManager.create(Building.class);
        building.setName("Здание " + suffix);

        floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(1);

        room = dataManager.create(Room.class);
        room.setFloor(floor);
        room.setName("Переговорка");
        room.setCapacity(6);

        desk = dataManager.create(Desk.class);
        desk.setFloor(floor);
        desk.setCode("A-1");

        dataManager.save(building, floor, room, desk);

        cleanup.add(building);
        cleanup.add(user);
    }

    private void givenRoomAmenity() {
        amenity = dataManager.create(Amenity.class);
        amenity.setName("Проектор " + UUID.randomUUID());
        amenity = dataManager.save(amenity);
        cleanup.add(amenity);

        Room loaded = loadRoomWithAmenities();
        loaded.getAmenities().add(amenity);
        dataManager.save(loaded);
    }

    private Room loadRoomWithAmenities() {
        return dataManager.load(Room.class)
                .id(room.getId())
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("amenities"))
                .one();
    }

    private Booking givenBooking(Room room, Desk desk) {
        Booking booking = dataManager.create(Booking.class);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setDesk(desk);
        booking.setStartAt(LocalDateTime.of(2026, 10, 8, 10, 0));
        booking.setEndAt(LocalDateTime.of(2026, 10, 8, 11, 0));
        return dataManager.save(booking);
    }

    @AfterEach
    void tearDown() {
        if (amenity != null && room != null) {
            dataManager.load(Id.of(room)).optional().ifPresent(r -> {
                Room loaded = loadRoomWithAmenities();
                loaded.getAmenities().clear();
                dataManager.save(loaded);
            });
        }
        cleanup.forEach(entity -> dataManager.load(Id.of(entity)).optional().ifPresent(dataManager::remove));
        cleanup.clear();
    }
}
