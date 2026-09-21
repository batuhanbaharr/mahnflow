package com.mahnflow.application.service;

import com.mahnflow.application.port.in.InvoiceCommands.CreateInvoice;
import com.mahnflow.application.port.in.ManageInvoiceUseCase;
import com.mahnflow.application.port.out.InvoiceRepository;
import com.mahnflow.application.port.out.OutboxPort;
import com.mahnflow.domain.invoice.Invoice;
import com.mahnflow.domain.invoice.InvoiceEvent;
import com.mahnflow.domain.money.Money;
import com.mahnflow.statemachine.InvoiceTransition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// this service holds the use case logic. it only talks to ports (interfaces),
// so it does not know about postgres, redis or the web layer. that separation
// is the part of "hexagonal architecture" i was most curious to try out.
//
// each state-changing method does the same little dance: load -> ask the state
// machine if the move is allowed -> change the domain object -> save -> drop an
// outbox job. felt repetitive to write, but it keeps every operation consistent.
@Service
public class InvoiceService implements ManageInvoiceUseCase {

    private final InvoiceRepository invoices;
    private final OutboxPort outbox;
    private final InvoiceTransition transition;

    public InvoiceService(InvoiceRepository invoices, OutboxPort outbox,
                          InvoiceTransition transition) {
        this.invoices = invoices;
        this.outbox = outbox;
        this.transition = transition;
    }

    @Override
    @Transactional
    public Invoice createDraft(CreateInvoice command) {
        Money amount = Money.of(command.amount(), command.currency());
        Invoice invoice = Invoice.createDraft(command.customerId(), amount, command.dueDate());
        return invoices.save(invoice);
    }

    @Override
    @Transactional
    public Invoice issue(UUID invoiceId) {
        Invoice invoice = load(invoiceId);
        transition.check(invoice.status(), InvoiceEvent.ISSUE);
        invoice.issue();
        Invoice saved = invoices.save(invoice);
        // write the email job in the SAME transaction as the invoice change (outbox
        // pattern). this is the whole point: if the tx rolls back, the job is gone
        // too, so we never send a mail for something that didn't actually happen.
        // building the json by hand here is a bit ugly, ein richtiger event-payload
        // mit jackson wäre schöner. TODO later.
        outbox.append("INVOICE_ISSUED", saved.id(),
                "{\"invoiceId\":\"" + saved.id() + "\"}");
        return saved;
    }

    @Override
    @Transactional
    public Invoice pay(UUID invoiceId) {
        Invoice invoice = load(invoiceId);
        transition.check(invoice.status(), InvoiceEvent.PAY);
        invoice.pay();
        Invoice saved = invoices.save(invoice);
        outbox.append("INVOICE_PAID", saved.id(),
                "{\"invoiceId\":\"" + saved.id() + "\"}");
        return saved;
    }

    @Override
    @Transactional
    public Invoice cancel(UUID invoiceId) {
        Invoice invoice = load(invoiceId);
        transition.check(invoice.status(), InvoiceEvent.CANCEL);
        invoice.cancel();
        return invoices.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getById(UUID invoiceId) {
        return load(invoiceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getAll() {
        return invoices.findAll();
    }

    private Invoice load(UUID id) {
        return invoices.findById(id).orElseThrow(() -> new InvoiceNotFound(id));
    }
}
