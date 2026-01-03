package com.example.demo.client;


import com.example.demo.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Si vous utilisez Eureka, mettez juste le nom du service :
//FeignClient(name = "user-service")
// Si Eureka ne marche pas encore, utilisez l'URL en dur temporairement :
@FeignClient(name = "user-service", url = "http://localhost:8084")
public interface UserServiceClient {

    @PostMapping("/api/users/create-profile")
    void createUserProfile(@RequestBody UserDTO userDto);
}