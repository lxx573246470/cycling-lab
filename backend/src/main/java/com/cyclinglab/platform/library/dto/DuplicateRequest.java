package com.cyclinglab.platform.library.dto;

import jakarta.validation.constraints.Size;

public record DuplicateRequest(@Size(max = 128) String name) {}
