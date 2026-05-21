package org.gymbrot.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.FileInputStream;
import java.util.Properties;

public class SmsService {

    private final String accountSid;
    private final String authToken;
    private final String phoneNumber;

    public SmsService() {
        Properties config = cargarConfig();
        this.accountSid = config.getProperty("TWILIO_ACCOUNT_SID");
        this.authToken = config.getProperty("TWILIO_AUTH_TOKEN");
        this.phoneNumber = config.getProperty("TWILIO_PHONE_NUMBER");
        Twilio.init(accountSid, authToken);
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

    // ── ENVIAR SMS ────────────────────────────────────────────────────────
    public boolean enviarSms(String destinatario, String mensaje) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(destinatario),
                    new PhoneNumber(phoneNumber),
                    mensaje
            ).create();
            System.out.println("SMS enviado: " + message.getSid());
            return true;
        } catch (Exception e) {
            System.err.println("Error al enviar SMS: " + e.getMessage());
            return false;
        }
    }
}