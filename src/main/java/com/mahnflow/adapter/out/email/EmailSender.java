package com.mahnflow.adapter.out.email;

import java.util.UUID;

// port for sending mail. in a real app this would call a mail provider.
public interface EmailSender {

    void send(String type, UUID invoiceId, String payload);
}
