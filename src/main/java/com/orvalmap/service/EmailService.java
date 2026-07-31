package com.orvalmap.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class EmailService {

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${SENDER_EMAIL}")
    private String senderEmail;

    public void sendEmail(String to, String subject, String body) {
        Email from = new Email(senderEmail);
        Email toEmail = new Email(to);
        
        // --- CORRECTION : Utiliser du HTML pour le corps de l'e-mail ---
        // On garde le corps simple, mais on le met dans un format HTML
        // pour que le lien soit cliquable.
        Content content = new Content("text/html", body); 
        
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("E-mail envoyé à {}, statut: {}", to, response.getStatusCode());
        } catch (IOException ex) {
            log.error("Erreur lors de l'envoi de l'e-mail à {}", to, ex);
        }
    }
}
