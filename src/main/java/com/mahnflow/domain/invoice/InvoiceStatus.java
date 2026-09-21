package com.mahnflow.domain.invoice;

// all the states an invoice can be in during its life.
// the order here is also the normal happy path (top to bottom).
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    PAID,
    OVERDUE,
    DUNNING_LEVEL_1,
    DUNNING_LEVEL_2,
    CANCELLED
}
