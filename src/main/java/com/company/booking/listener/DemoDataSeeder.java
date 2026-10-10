package com.company.booking.listener;

import com.company.booking.service.DemoDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo office on startup, only in the dev profile.
 */
@Profile("dev")
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DemoDataService demoDataService;

    public DemoDataSeeder(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (demoDataService.seedIfEmpty()) {
            log.info("Demo data created");
        }
    }
}
