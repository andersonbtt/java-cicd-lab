package com.bittclouds.labjavacicd.task;

import java.time.Instant;
import java.util.UUID;

public record Task(
    String id,
    String title,
    TaskStatus status,
    Instant createdAt) {

  public static Task create(String title) {
    return new Task(UUID.randomUUID().toString(), title, TaskStatus.TODO, Instant.now());
  }
}
