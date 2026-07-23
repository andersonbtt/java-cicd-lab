package com.bittclouds.labjavacicd.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

  private static final Logger log = LoggerFactory.getLogger(TaskService.class);

  private final Map<String, Task> tasks = new ConcurrentHashMap<>();
  private final AtomicLong createdCount = new AtomicLong();
  private final AtomicLong listedCount = new AtomicLong();

  public List<Task> list(TaskStatus statusFilter) {
    long n = listedCount.incrementAndGet();
    List<Task> result = tasks.values().stream()
        .filter(task -> statusFilter == null || task.status() == statusFilter)
        .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
        .toList();

    log.info(
        "event=task_listed totalReturned={} statusFilter={} listedOperations={}",
        result.size(),
        statusFilter == null ? "ALL" : statusFilter.name(),
        n);
    return List.copyOf(result);
  }

  public Task create(String title) {
    Task task = Task.create(title.trim());
    tasks.put(task.id(), task);
    long n = createdCount.incrementAndGet();
    log.info(
        "event=task_created taskId={} titleLength={} createdOperations={}",
        task.id(),
        task.title().length(),
        n);
    return task;
  }

  public Task getById(String id) {
    return Optional.ofNullable(tasks.get(id))
        .orElseThrow(() -> new TaskNotFoundException(id));
  }

  public Map<String, Long> stats() {
    return Map.of(
        "createdOperations", createdCount.get(),
        "listedOperations", listedCount.get(),
        "tasksStored", (long) tasks.size());
  }

  /** Usado só em testes para isolar o estado in-memory. */
  void clear() {
    tasks.clear();
    createdCount.set(0);
    listedCount.set(0);
  }
}
