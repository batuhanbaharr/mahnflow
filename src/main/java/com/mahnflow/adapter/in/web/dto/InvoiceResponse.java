package com.mahnflow.adapter.in.web.dto;

import com.mahnflow.domain.invoice.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// what we send back to the client. we never return the domain object directly.
public record InvoiceResponse(
        UUID id,
        UUID customerId,
        BigDecimal amount,
        String currency,
        LocalDate dueDate,
        String status,
        Instant issuedAt,
        Instant paidAt,
        long version) {

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.id(),
                invoice.customerId(),
                invoice.amount().amount(),
                invoice.amount().currency().getCurrencyCode(),
                invoice.dueDate(),
                invoice.status().name(),
                invoice.issuedAt(),
                invoice.paidAt(),
                invoice.version());
    }
}
