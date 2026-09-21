package com.mahnflow.application.port.out;

import com.mahnflow.domain.invoice.Invoice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// port: how the application saves and loads invoices.
// the real database code lives in the adapter layer and implements this.
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    List<Invoice> findAll();

    // all issued invoices whose due date is before the given day.
    // used by the background job that marks them overdue.
    List<Invoice> findIssuedDueBefore(LocalDate day);

    // invoices that already sit in a dunning or overdue state,
    // so the job can push them to the next reminder level.
    List<Invoice> findInDunningFlow();
}
