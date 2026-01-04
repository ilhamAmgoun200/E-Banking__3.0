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

    // ---------------- Twilio credentials ----------------
    private final String TWILIO_SID = "AC3ae7f397a77a0350ff74987ab481c04b";
    private final String TWILIO_AUTH_TOKEN = "ca71bfb3abe176206de34f46466a77a9";
    private final String TWILIO_NUMBER = "+17756288872";

    // ---------------- Main notification method ----------------
    public void sendNotification(NotificationRequest request) {

        String email = request.getEmail();
        String phoneNumber = request.getPhoneNumber();
        String messageText = request.getMessage();

        // -------- Normalize Moroccan number to E.164 --------
        if (phoneNumber != null && phoneNumber.startsWith("0")) {
            phoneNumber = "+212" + phoneNumber.substring(1);
        }

        // -------- Send email --------
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

        // -------- Send SMS via Twilio --------
        try {
            Twilio.init(TWILIO_SID, TWILIO_AUTH_TOKEN);
            Message.creator(
                    new PhoneNumber(phoneNumber),      // to
                    new PhoneNumber(TWILIO_NUMBER),   // from (Twilio number)
                    messageText
            ).create();

            System.out.println("✅ SMS sent to: " + phoneNumber);
        } catch (Exception e) {
            System.out.println("❌ Failed to send SMS: " + e.getMessage());
        }
    }
}
