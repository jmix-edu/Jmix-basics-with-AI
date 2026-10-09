package com.company.booking.floorplan;

import com.company.booking.entity.Building;
import com.company.booking.entity.Floor;
import com.company.booking.service.FloorPlanValidationException;
import com.company.booking.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Server-side plan type check from specs/03_files/01-floor-plan.spec.adoc. Only the
 * file name matters, so the references point to no stored file.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@ActiveProfiles("test")
public class FloorPlanTest {

    @Autowired
    DataManager dataManager;

    Building building;

    @BeforeEach
    void setUp() {
        building = dataManager.create(Building.class);
        building.setName("Здание " + UUID.randomUUID());
        building = dataManager.save(building);
    }

    @AfterEach
    void tearDown() {
        // Building delete cascades to its floors
        dataManager.load(Id.of(building)).optional().ifPresent(dataManager::remove);
    }

    @Test
    void pngPlanIsSaved() {
        assertSaved(floor(1, "plan.png"));
    }

    @Test
    void upperCaseJpegPlanIsSaved() {
        assertSaved(floor(2, "plan.JPEG"));
    }

    @Test
    void floorWithoutPlanIsSaved() {
        assertSaved(floor(3, null));
    }

    @Test
    void pdfPlanIsRejected() {
        Floor floor = floor(4, "plan.pdf");

        assertThatThrownBy(() -> dataManager.save(floor))
                .isInstanceOf(FloorPlanValidationException.class)
                .hasMessage("Можно загрузить только PNG или JPEG.");
        assertThat(dataManager.load(Id.of(floor)).optional()).isEmpty();
    }

    private Floor floor(int number, String planFileName) {
        Floor floor = dataManager.create(Floor.class);
        floor.setBuilding(building);
        floor.setNumber(number);
        if (planFileName != null) {
            floor.setPlan(FileRef.create("fs", "2026/10/09/" + UUID.randomUUID() + "-" + planFileName, planFileName));
        }
        return floor;
    }

    private void assertSaved(Floor floor) {
        dataManager.saveWithoutReload(floor);
        assertThat(dataManager.load(Id.of(floor)).optional()).isPresent();
    }
}
