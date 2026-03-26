package com.nexus.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // @Async tells Spring to run this in the background so the user's web page loads instantly
    @Async
    public void sendRegistrationEmail(String to, String studentName, String eventTitle, String ticketNumber, boolean isWaitlisted) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(isWaitlisted ? "Waitlist Notification: " + eventTitle : "Ticket Confirmed: " + eventTitle);

            String statusHtml = isWaitlisted 
                ? "<span style='color: #d97706; font-weight: bold;'>Waitlisted</span>" 
                : "<span style='color: #16a34a; font-weight: bold;'>Confirmed</span>";

            String qrCodeHtml = isWaitlisted 
                ? "<p style='color: #555;'>You are currently on the waitlist. We will notify you if a spot opens up.</p>"
                : "<p>Present this QR Code at the entrance:</p><br><img src='https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + ticketNumber + "' alt='QR Code' style='border: 2px solid #000; border-radius: 8px;'>";

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #000; background-color: #f8fafc; border-radius: 8px; max-width: 600px; margin: auto;'>"
                    + "<h2 style='color: #e50914; border-bottom: 2px solid #e2e8f0; padding-bottom: 10px;'>VEL TECH Events</h2>"
                    + "<p>Hi <strong>" + studentName + "</strong>,</p>"
                    + "<p>Here is your registration update for <strong>" + eventTitle + "</strong>.</p>"
                    + "<p>Status: " + statusHtml + "</p>"
                    + "<p>Ticket ID: <strong style='font-family: monospace; font-size: 1.2rem; background: #e2e8f0; padding: 4px 8px; border-radius: 4px;'>" + ticketNumber + "</strong></p>"
                    + qrCodeHtml
                    + "<p style='margin-top: 30px; border-top: 1px solid #e2e8f0; padding-top: 15px; font-size: 0.9rem; color: #666;'>See you there!<br>Vel Tech Event Organizers</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            System.out.println("CRITICAL ERROR: Failed to send email to " + to);
            e.printStackTrace();
        }
    }
}