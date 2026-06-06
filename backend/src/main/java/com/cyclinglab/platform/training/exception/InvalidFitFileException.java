package com.cyclinglab.platform.training.exception;

/** Thrown when the uploaded file is not a parseable FIT file. */
public class InvalidFitFileException extends RuntimeException {

    public InvalidFitFileException(String message) {
        super(message);
    }

    public InvalidFitFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
