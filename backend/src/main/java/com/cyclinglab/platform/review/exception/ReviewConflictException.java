package com.cyclinglab.platform.review.exception;

/** Thrown when the same (scope, week) is created twice, etc. */
public class ReviewConflictException extends RuntimeException {

    public ReviewConflictException(String message) {
        super(message);
    }
}