package com.bittclouds.labjavacicd.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TaskService taskService;

  @BeforeEach
  void setUp() {
    taskService.clear();
  }

  @Test
  void createAndListTasks() throws Exception {
    mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Write docs\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Write docs"))
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.id").isNotEmpty());

    mockMvc.perform(get("/api/tasks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Write docs"));
  }

  @Test
  void createRejectsBlankTitle() throws Exception {
    mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation_failed"))
        .andExpect(jsonPath("$.details.title").exists());
  }

  @Test
  void getByIdReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/tasks/missing-id"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("task_not_found"));
  }

  @Test
  void getByIdReturnsCreatedTask() throws Exception {
    MvcResult created = mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Ship feature\"}"))
        .andExpect(status().isCreated())
        .andReturn();

    String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

    mockMvc.perform(get("/api/tasks/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Ship feature"));
  }

  @Test
  void statsIncreaseAfterOperations() throws Exception {
    mockMvc.perform(post("/api/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Count me\"}"))
        .andExpect(status().isCreated());
    mockMvc.perform(get("/api/tasks")).andExpect(status().isOk());

    mockMvc.perform(get("/api/tasks/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdOperations").value(1))
        .andExpect(jsonPath("$.listedOperations").value(1))
        .andExpect(jsonPath("$.tasksStored").value(1));
  }
}
