package com.mahnflow.application.port.out;

import java.util.UUID;

// port: the application drops a message here (in the same db transaction) and a
// background worker reads it later and sends the email / makes the pdf.
// this is the transactional outbox pattern, the trick that finally made the
// "what if the mail server is down but the invoice already saved?" problem click
// for me. the service only knows this interface, not how it's stored.
public interface OutboxPort {

    void append(String type, UUID invoiceId, String payloadJson);
}
