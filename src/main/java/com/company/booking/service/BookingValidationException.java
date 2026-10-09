package com.company.booking.service;

import java.util.List;

/**
 * Thrown when a booking being saved violates the rules of {@link BookingValidationService}.
 */
public class BookingValidationException extends RuntimeException {

    private final List<String> errors;

    public BookingValidationException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
