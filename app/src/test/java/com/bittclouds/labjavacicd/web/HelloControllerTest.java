package com.bittclouds.labjavacicd.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void helloReturnsOkMessage() throws Exception {
    mockMvc.perform(get("/api/hello"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Hello from labjavacicd"))
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(header().exists(RequestIdFilter.REQUEST_ID_HEADER));
  }

  @Test
  void helloPropagatesIncomingRequestId() throws Exception {
    mockMvc.perform(get("/api/hello").header(RequestIdFilter.REQUEST_ID_HEADER, "test-req-123"))
        .andExpect(status().isOk())
        .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, "test-req-123"));
  }
}
