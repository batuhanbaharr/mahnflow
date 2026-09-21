package com.mahnflow.domain.invoice;

import com.mahnflow.domain.money.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// the main domain object. it keeps its own rules so a bad state change
// can never happen, no matter who calls it. spring is not imported here on
// purpose — i wanted to try the "keep the domain pure" idea i read about in a
// few clean-architecture blog posts.
//
// yes, the state rules also live in the spring statemachine config, so there is
// some overlap. i kept the guards here too as a safety net (defense in depth):
// even if some future code forgets to ask the state machine first, the object
// still refuses to go into a wrong state. hope that's a reasonable call.
public class Invoice {

    private final UUID id;
    private final UUID customerId;
    private final Money amount;
    private final LocalDate dueDate;
    private InvoiceStatus status;
    private Instant issuedAt;
    private Instant paidAt;
    private long version;

    public Invoice(UUID id, UUID customerId, Money amount, LocalDate dueDate,
                   InvoiceStatus status, Instant issuedAt, Instant paidAt, long version) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.amount = Objects.requireNonNull(amount);
        this.dueDate = Objects.requireNonNull(dueDate);
        this.status = Objects.requireNonNull(status);
        this.issuedAt = issuedAt;
        this.paidAt = paidAt;
        this.version = version;
    }

    // make a brand new invoice in DRAFT state.
    public static Invoice createDraft(UUID customerId, Money amount, LocalDate dueDate) {
        return new Invoice(UUID.randomUUID(), customerId, amount, dueDate,
                InvoiceStatus.DRAFT, null, null, 0);
    }

    // --- state changes. each one checks if the move is allowed first. ---

    public void issue() {
        require(status == InvoiceStatus.DRAFT, InvoiceEvent.ISSUE);
        this.status = InvoiceStatus.ISSUED;
        this.issuedAt = Instant.now();
    }

    public void pay() {
        // you can pay from any open state, but not from draft, cancelled or already paid.
        boolean canPay = status == InvoiceStatus.ISSUED
                || status == InvoiceStatus.OVERDUE
                || status == InvoiceStatus.DUNNING_LEVEL_1
                || status == InvoiceStatus.DUNNING_LEVEL_2;
        require(canPay, InvoiceEvent.PAY);
        this.status = InvoiceStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void markOverdue() {
        require(status == InvoiceStatus.ISSUED, InvoiceEvent.MARK_OVERDUE);
        this.status = InvoiceStatus.OVERDUE;
    }

    public void sendFirstReminder() {
        require(status == InvoiceStatus.OVERDUE, InvoiceEvent.SEND_FIRST_REMINDER);
        this.status = InvoiceStatus.DUNNING_LEVEL_1;
    }

    public void sendSecondReminder() {
        require(status == InvoiceStatus.DUNNING_LEVEL_1, InvoiceEvent.SEND_SECOND_REMINDER);
        this.status = InvoiceStatus.DUNNING_LEVEL_2;
    }

    public void cancel() {
        // a paid invoice can not be cancelled, that would break the books.
        require(status != InvoiceStatus.PAID && status != InvoiceStatus.CANCELLED,
                InvoiceEvent.CANCEL);
        this.status = InvoiceStatus.CANCELLED;
    }

    // is this invoice past its due date and still open?
    public boolean isPastDue(LocalDate today) {
        return status == InvoiceStatus.ISSUED && dueDate.isBefore(today);
    }

    private void require(boolean ok, InvoiceEvent event) {
        if (!ok) {
            throw new InvalidStateChange(status, event);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public Money amount() {
        return amount;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public InvoiceStatus status() {
        return status;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant paidAt() {
        return paidAt;
    }

    public long version() {
        return version;
    }
}
