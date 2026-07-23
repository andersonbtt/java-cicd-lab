package com.bittclouds.labjavacicd.task;

public class TaskNotFoundException extends RuntimeException {

  private final String taskId;

  public TaskNotFoundException(String taskId) {
    super("Task not found: " + taskId);
    this.taskId = taskId;
  }

  public String getTaskId() {
    return taskId;
  }
}
