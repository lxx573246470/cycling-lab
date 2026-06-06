package com.cyclinglab.platform.library.exception;

import java.util.List;

/**
 * Thrown when {@code structure_json} fails structural validation. The
 * global exception handler maps this to HTTP 422 with code
 * {@code UNPROCESSABLE_STRUCTURE} and a {@code details[]} carrying JSON
 * Pointers (see doc/ARCHITECTURE.md §15.1.3).
 */
public class StructureValidationException extends RuntimeException {

    public record Detail(String pointer, String message) {}

    private final List<Detail> details;

    public StructureValidationException(List<Detail> details) {
        super("Invalid structure_json");
        this.details = details;
    }

    public List<Detail> getDetails() {
        return details;
    }
}
