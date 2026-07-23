package com.bittclouds.labjavacicd.task;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public List<Task> list(@RequestParam(required = false) TaskStatus status) {
    return taskService.list(status);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Task create(@Valid @RequestBody CreateTaskRequest request) {
    return taskService.create(request.title());
  }

  @GetMapping("/stats")
  public Map<String, Long> stats() {
    return taskService.stats();
  }

  @GetMapping("/{id}")
  public Task getById(@PathVariable String id) {
    return taskService.getById(id);
  }
}