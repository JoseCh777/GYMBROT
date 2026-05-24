package org.gymbrot.service;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.FileInputStream;
import java.util.Properties;

public class EmailService {

    private final String email;
    private final String password;

    public EmailService() {
        Properties config = cargarConfig();
        this.email = config.getProperty("GMAIL");
        this.password = config.getProperty("GMAIL_PASSWORD");
    }

    private boolean configValida() {
        return email != null && !email.isEmpty() && password != null && !password.isEmpty();
    }

    // ── CARGAR CONFIG ─────────────────────────────────────────────────────
    private Properties cargarConfig() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("config.properties"));
        } catch (Exception e) {
            System.err.println("Error al cargar config.properties: " + e.getMessage());
        }
        return props;
    }

    // ── CONFIGURAR SESION SMTP ────────────────────────────────────────────
    private Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    public boolean enviarCorreo(String destinatario, String asunto, String contenido) {
        if (!configValida()) {
            System.err.println("⚠ Email no configurado — revisa config.properties");
            return false;
        }
        try {
            Message message = new MimeMessage(getSession());
            message.setFrom(new InternetAddress(email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(asunto);
            message.setContent(contenido, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("Correo enviado a: " + destinatario);
            return true;
        } catch (MessagingException e) {
            System.err.println("Error al enviar correo: " + e.getMessage());
            return false;
        }
    }
}