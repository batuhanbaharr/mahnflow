package com.mahnflow;

import com.mahnflow.domain.invoice.InvalidStateChange;
import com.mahnflow.domain.invoice.Invoice;
import com.mahnflow.domain.invoice.InvoiceStatus;
import com.mahnflow.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// plain unit tests for the domain rules. no spring, no database, very fast.
class InvoiceDomainTest {

    private Invoice newDraft() {
        return Invoice.createDraft(UUID.randomUUID(), Money.euro("100.00"),
                LocalDate.now().plusDays(14));
    }

    @Test
    void happy_path_goes_draft_to_issued_to_paid() {
        Invoice invoice = newDraft();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.DRAFT);

        invoice.issue();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.ISSUED);

        invoice.pay();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void can_not_pay_a_draft() {
        Invoice invoice = newDraft();
        assertThatThrownBy(invoice::pay).isInstanceOf(InvalidStateChange.class);
    }

    @Test
    void can_not_go_back_from_paid() {
        Invoice invoice = newDraft();
        invoice.issue();
        invoice.pay();
        // paid is final, issuing again must fail
        assertThatThrownBy(invoice::issue).isInstanceOf(InvalidStateChange.class);
    }

    @Test
    void can_not_cancel_a_paid_invoice() {
        Invoice invoice = newDraft();
        invoice.issue();
        invoice.pay();
        assertThatThrownBy(invoice::cancel).isInstanceOf(InvalidStateChange.class);
    }
}
