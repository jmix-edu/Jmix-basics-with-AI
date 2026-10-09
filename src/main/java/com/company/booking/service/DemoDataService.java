package com.company.booking.service;

import com.company.booking.entity.Building;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Floor;
import com.company.booking.entity.Room;
import io.jmix.core.DataManager;
import io.jmix.core.security.Authenticated;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo office: one building with two floors, rooms and desks.
 */
@Service
public class DemoDataService {

    private final DataManager dataManager;

    public DemoDataService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Creates the demo office if the database has no buildings yet.
     * Runs under the system user, so it works on the startup thread.
     *
     * @return true if the demo data was created
     */
    @Authenticated
    public boolean seedIfEmpty() {
        if (dataManager.load(Building.class).all().maxResults(1).optional().isPresent()) {
            return false;
        }

        List<Object> entities = new ArrayList<>();
        Building building = dataManager.create(Building.class);
        building.setName("БЦ «Северный»");
        building.setAddress("Москва, ул. Лесная, 5");
        entities.add(building);

        Floor first = floor(building, 1, "Ресепшн и переговорки", entities);
        room(first, "Альфа", 8, entities);
        room(first, "Бета", 4, entities);
        desks(first, "A", 4, entities);

        Floor second = floor(building, 2, "Открытое пространство", entities);
        room(second, "Гамма", 12, entities);
        room(second, "Фокус", 2, entities);
        desks(second, "B", 8, entities);

        dataManager.saveWithoutReload(entities.toArray());
        return true;
    }

    private Floor floor(Building building, int number, String name, List<Object> entities) {
        Floor floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(number);
        floor.setName(name);
        entities.add(floor);
        return floor;
    }

    private void room(Floor floor, String name, int capacity, List<Object> entities) {
        Room room = dataManager.create(Room.class);
        room.setFloor(floor);
        room.setName(name);
        room.setCapacity(capacity);
        entities.add(room);
    }

    private void desks(Floor floor, String prefix, int count, List<Object> entities) {
        for (int i = 1; i <= count; i++) {
            Desk desk = dataManager.create(Desk.class);
            desk.setFloor(floor);
            desk.setCode(prefix + "-" + i);
            entities.add(desk);
        }
    }
}
