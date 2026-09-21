package com.mahnflow.domain.invoice;

// the things that can happen to an invoice. these drive the state machine.
public enum InvoiceEvent {
    ISSUE,
    PAY,
    MARK_OVERDUE,
    SEND_FIRST_REMINDER,
    SEND_SECOND_REMINDER,
    CANCEL
}
