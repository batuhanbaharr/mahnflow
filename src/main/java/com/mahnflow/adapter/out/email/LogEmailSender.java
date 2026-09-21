package com.mahnflow.adapter.out.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

// simple sender that just writes to the log.
// it stands in for a real mail / pdf service so the demo runs with no extra setup.
@Component
public class LogEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(String type, UUID invoiceId, String payload) {
        log.info("sending mail type={} invoice={} payload={}", type, invoiceId, payload);
    }
}
