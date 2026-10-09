package com.company.booking.service;

/**
 * Thrown when a floor is saved with a plan that is not a PNG or JPEG file.
 */
public class FloorPlanValidationException extends RuntimeException {

    public FloorPlanValidationException(String message) {
        super(message);
    }
}
