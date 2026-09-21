package com.mahnflow.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// what the client sends to make a new invoice. validated before it reaches the service.
public record CreateInvoiceRequest(
        @NotNull UUID customerId,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate dueDate) {
}
