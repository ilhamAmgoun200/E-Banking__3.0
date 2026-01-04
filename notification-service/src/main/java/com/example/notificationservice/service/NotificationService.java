package com.onlinebanking.notificationservice.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.onlinebanking.notificationservice.dto.NotificationRequest;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
public class NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    // ---------------- Twilio credentials from environment variables ----------------
    private final String TWILIO_SID = System.getenv("TWILIO_SID");
    private final String TWILIO_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    private final String TWILIO_NUMBER = System.getenv("TWILIO_NUMBER");

    public void sendNotification(NotificationRequest request) {

        String email = request.getEmail();
        String phoneNumber = request.getPhoneNumber();
        String messageText = request.getMessage();

        if (phoneNumber != null && phoneNumber.startsWith("0")) {
            phoneNumber = "+212" + phoneNumber.substring(1);
        }

        // Send email
        try {
            SimpleMailMessage emailMessage = new SimpleMailMessage();
            emailMessage.setTo(email);
            emailMessage.setSubject("Transaction Notification");
            emailMessage.setText(messageText);
            mailSender.send(emailMessage);

            System.out.println("✅ Email sent to: " + email);
        } catch (Exception e) {
            System.out.println("❌ Failed to send email: " + e.getMessage());
        }

        // Send SMS via Twilio
        try {
            Twilio.init(TWILIO_SID, TWILIO_AUTH_TOKEN);
            Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(TWILIO_NUMBER),
                    messageText
            ).create();

            System.out.println("✅ SMS sent to: " + phoneNumber);
        } catch (Exception e) {
            System.out.println("❌ Failed to send SMS: " + e.getMessage());
        }
    }
}
