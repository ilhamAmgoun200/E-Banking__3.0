package com.onlinebanking.notificationservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.onlinebanking.notificationservice.dto.NotificationRequest;
import com.onlinebanking.notificationservice.service.NotificationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public void notifyUser(@RequestBody NotificationRequest request) {
        notificationService.sendNotification(request);
    }
}
