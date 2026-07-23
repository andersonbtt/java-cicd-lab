package com.bittclouds.labjavacicd.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotBlank(message = "title is required")
    @Size(min = 3, max = 120, message = "title must be between 3 and 120 characters")
    String title) {
}
