package com.bittclouds.labjavacicd.web;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

  private static final Logger log = LoggerFactory.getLogger(HelloController.class);

  @GetMapping("/hello")
  public Map<String, String> hello() {
    log.info("Handling hello request");
    return Map.of(
        "message", "Hello from labjavacicd",
        "status", "ok");
  }
}
