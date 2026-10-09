package com.company.booking.listener;

import com.company.booking.service.DemoDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo office on startup; switched off with booking.demo-data.enabled=false.
 */
@Component
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DemoDataService demoDataService;
    private final boolean enabled;

    public DemoDataSeeder(DemoDataService demoDataService,
                          @Value("${booking.demo-data.enabled:true}") boolean enabled) {
        this.demoDataService = demoDataService;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (enabled && demoDataService.seedIfEmpty()) {
            log.info("Demo data created");
        }
    }
}
