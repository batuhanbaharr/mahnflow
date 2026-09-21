package com.mahnflow.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// small input objects (commands) for the use case.
// records keep them short and read only.
public final class InvoiceCommands {

    private InvoiceCommands() {
    }

    public record CreateInvoice(UUID customerId, BigDecimal amount,
                                String currency, LocalDate dueDate) {
    }
}
