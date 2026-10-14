package com.hrcoresystem.hrcore_api.bootstrap.platform;

import com.hrcoresystem.hrcore_api.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicTestController {

  @GetMapping("/ping")
  public ApiResponse<Map<String, String>> ping() {
    return ApiResponse.success(
        "HRCore API is running correctly",
        Map.of("status", "ok"));
  }

}
