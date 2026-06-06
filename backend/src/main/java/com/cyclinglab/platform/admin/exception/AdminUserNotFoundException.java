package com.cyclinglab.platform.admin.exception;

import java.util.UUID;

public class AdminUserNotFoundException extends RuntimeException {

    private final UUID id;

    public AdminUserNotFoundException(UUID id) {
        super("User not found: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}