package com.onlinebanking.uiservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "account-service", url = "http://localhost:8081")
public interface AccountServiceClient {
    @PostMapping("/accounts")
    Map<String, Object> createAccount(@RequestBody Map<String, Object> account);
}
