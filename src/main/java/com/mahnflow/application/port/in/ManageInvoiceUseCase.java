package com.mahnflow.application.port.in;

import com.mahnflow.application.port.in.InvoiceCommands.CreateInvoice;
import com.mahnflow.domain.invoice.Invoice;

import java.util.List;
import java.util.UUID;

// port: what the outside world (the rest controller) is allowed to ask for.
// the idea i'm copying from the hexagonal posts: the controller depends on this
// interface, not on the concrete service. makes it easy to fake in tests too.
public interface ManageInvoiceUseCase {

    Invoice createDraft(CreateInvoice command);

    Invoice issue(UUID invoiceId);

    Invoice pay(UUID invoiceId);

    Invoice cancel(UUID invoiceId);

    Invoice getById(UUID invoiceId);

    List<Invoice> getAll();
}
