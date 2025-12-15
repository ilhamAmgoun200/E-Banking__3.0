package com.onlinebanking.uiservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8084")
public interface UserServiceClient {
    @PostMapping("/api/register")
    Map<String, Object> register(@RequestBody Map<String, String> user);

    @PostMapping("/api/login")
    Map<String, Object> login(@RequestBody Map<String, String> user);
}
