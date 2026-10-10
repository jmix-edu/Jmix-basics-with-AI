package com.company.booking.demodata;

import com.company.booking.entity.Building;
import com.company.booking.entity.Desk;
import com.company.booking.entity.Room;
import com.company.booking.listener.DemoDataSeeder;
import com.company.booking.service.DemoDataService;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calls the seeder with no authentication set up, as the startup thread does.
 */
@SpringBootTest
@ActiveProfiles("test")
public class DemoDataServiceTest {

    @Autowired
    DemoDataService demoDataService;
    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        assertThat(count(Building.class))
                .as("test database must have no buildings before the seeder runs")
                .isZero();
    }

    @AfterEach
    void tearDown() {
        // Only the demo building; its delete cascades to floors, rooms and desks
        systemAuthenticator.runWithSystem(() ->
                dataManager.load(Building.class)
                        .query("select e from Building e where e.name = :name")
                        .parameter("name", "БЦ «Северный»")
                        .list()
                        .forEach(dataManager::remove));
    }

    @Test
    void seedsEmptyDatabaseOnceOnly() {
        assertThat(demoDataService.seedIfEmpty()).isTrue();
        assertThat(count(Building.class)).isEqualTo(1);
        assertThat(count(Room.class)).isEqualTo(4);
        assertThat(count(Desk.class)).isEqualTo(12);

        assertThat(demoDataService.seedIfEmpty()).isFalse();
        assertThat(count(Building.class)).isEqualTo(1);
        assertThat(count(Room.class)).isEqualTo(4);
        assertThat(count(Desk.class)).isEqualTo(12);
    }

    @Test
    void seederIsNotCreatedOutsideDevProfile() {
        // Tests run in the test profile: the startup seeder must not be created here, as in prod
        assertThat(applicationContext.getBeansOfType(DemoDataSeeder.class)).isEmpty();
    }

    private long count(Class<?> entityClass) {
        return systemAuthenticator.withSystem(() ->
                (long) dataManager.load(entityClass).all().list().size());
    }
}
