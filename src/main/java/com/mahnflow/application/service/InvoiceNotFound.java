package com.mahnflow.application.service;

import java.util.UUID;

// thrown when an invoice id does not exist in the database.
public class InvoiceNotFound extends RuntimeException {

    public InvoiceNotFound(UUID id) {
        super("invoice not found: " + id);
    }
}
