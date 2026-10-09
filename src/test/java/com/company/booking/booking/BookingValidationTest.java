package com.company.booking.booking;

import com.company.booking.entity.*;
import com.company.booking.service.BookingValidationException;
import com.company.booking.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Booking rules R1-R3 from specs/01_foundations/03-slot-conflict-service.spec.adoc,
 * checked through a plain DataManager save (the BookingSavingListener path).
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@ActiveProfiles("test")
public class BookingValidationTest {

    static final LocalDateTime DAY = LocalDateTime.of(2026, 10, 8, 0, 0);

    @Autowired
    DataManager dataManager;

    User user;
    Building building;
    Room room;
    Room otherRoom;
    Desk desk;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();

        user = dataManager.create(User.class);
        user.setUsername("booking-validation-" + suffix);
        user.setFirstName("Тест");
        user = dataManager.save(user);

        building = dataManager.create(Building.class);
        building.setName("Здание " + suffix);

        Floor floor = dataManager.create(Floor.class);
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

        dataManager.save(building, floor, room, otherRoom, desk);
    }

    @AfterEach
    void tearDown() {
        // Building delete cascades to floors, rooms, desks and their bookings
        dataManager.load(Id.of(building)).optional().ifPresent(dataManager::remove);
        dataManager.load(Id.of(user)).optional().ifPresent(dataManager::remove);
    }

    @Test
    void endEqualToStartIsRejected() {
        assertRejected(booking(room, null, 10, 0, 10, 0), "Окончание брони должно быть позже начала.");
    }

    @Test
    void endBeforeStartIsRejected() {
        assertRejected(booking(room, null, 11, 0, 10, 0), "Окончание брони должно быть позже начала.");
    }

    @Test
    void bookingWithoutTargetIsRejected() {
        assertRejected(booking(null, null, 10, 0, 11, 0), "Укажите переговорку или рабочее место.");
    }

    @Test
    void bookingWithBothTargetsIsRejected() {
        assertRejected(booking(room, desk, 10, 0, 11, 0),
                "Бронь может быть либо на переговорку, либо на рабочее место, но не на оба сразу.");
    }

    @Test
    void overlappingRoomBookingIsRejected() {
        dataManager.save(booking(room, null, 10, 0, 11, 0));

        assertRejected(booking(room, null, 10, 30, 11, 30),
                "Переговорка «Альфа» уже забронирована на это время: 08.10.2026 10:00 – 11:00.");
    }

    @Test
    void deskBookingInsideExistingOneIsRejected() {
        dataManager.save(booking(null, desk, 9, 0, 13, 0));

        assertRejected(booking(null, desk, 10, 0, 11, 0),
                "Рабочее место «A-1» уже забронировано на это время: 08.10.2026 09:00 – 13:00.");
    }

    @Test
    void adjacentBookingsAreAllowed() {
        dataManager.save(booking(room, null, 10, 0, 11, 0));

        assertSaved(booking(room, null, 11, 0, 12, 0));
    }

    @Test
    void overlapWithCancelledBookingIsAllowed() {
        Booking cancelled = booking(room, null, 10, 0, 11, 0);
        cancelled.setStatus(BookingStatus.CANCELLED);
        dataManager.save(cancelled);

        assertSaved(booking(room, null, 10, 0, 11, 0));
    }

    @Test
    void overlapWithCompletedBookingIsRejected() {
        Booking completed = booking(room, null, 10, 0, 11, 0);
        completed.setStatus(BookingStatus.COMPLETED);
        dataManager.save(completed);

        assertRejected(booking(room, null, 10, 0, 11, 0),
                "Переговорка «Альфа» уже забронирована на это время: 08.10.2026 10:00 – 11:00.");
    }

    @Test
    void cancelledBookingOverBusySlotIsAllowed() {
        dataManager.save(booking(room, null, 10, 0, 11, 0));

        Booking cancelled = booking(room, null, 10, 0, 11, 0);
        cancelled.setStatus(BookingStatus.CANCELLED);
        assertSaved(cancelled);
    }

    @Test
    void updatingBookingWithoutTimeChangeIsAllowed() {
        Booking saved = dataManager.save(booking(room, null, 10, 0, 11, 0));

        // Default _base plan leaves room unfetched: the service must resolve it itself
        Booking loaded = dataManager.load(Booking.class).id(saved.getId()).one();
        loaded.setComment("Перенесли повестку");
        assertSaved(loaded);
    }

    @Test
    void sameTimeInOtherRoomIsAllowed() {
        dataManager.save(booking(room, null, 10, 0, 11, 0));

        assertSaved(booking(otherRoom, null, 10, 0, 11, 0));
    }

    private Booking booking(Room room, Desk desk, int startHour, int startMinute, int endHour, int endMinute) {
        Booking booking = dataManager.create(Booking.class);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setDesk(desk);
        booking.setStartAt(DAY.withHour(startHour).withMinute(startMinute));
        booking.setEndAt(DAY.withHour(endHour).withMinute(endMinute));
        return booking;
    }

    private void assertRejected(Booking booking, String expectedMessage) {
        assertThatThrownBy(() -> dataManager.save(booking))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining(expectedMessage);
        assertThat(dataManager.load(Id.of(booking)).optional()).isEmpty();
    }

    private void assertSaved(Booking booking) {
        dataManager.save(booking);
        assertThat(dataManager.load(Id.of(booking)).optional()).isPresent();
    }
}
