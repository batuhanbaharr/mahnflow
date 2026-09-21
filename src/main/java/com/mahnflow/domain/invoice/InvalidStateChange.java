package com.mahnflow.domain.invoice;

// thrown when someone tries a move that is not allowed,
// for example going from PAID back to DRAFT.
public class InvalidStateChange extends RuntimeException {

    public InvalidStateChange(InvoiceStatus from, InvoiceEvent event) {
        super("can not do " + event + " when invoice is " + from);
    }
}
