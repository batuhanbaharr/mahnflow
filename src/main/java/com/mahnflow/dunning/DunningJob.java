package com.mahnflow.dunning;

import com.mahnflow.application.port.out.InvoiceRepository;
import com.mahnflow.application.port.out.OutboxPort;
import com.mahnflow.domain.invoice.Invoice;
import com.mahnflow.domain.invoice.InvoiceEvent;
import com.mahnflow.domain.invoice.InvoiceStatus;
import com.mahnflow.statemachine.InvoiceTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// the heart of "mahnwesen". on a schedule it:
//  1. marks issued invoices overdue once their due date has passed
//  2. pushes overdue invoices to reminder level 1, then level 2
// each reminder drops a job in the outbox so the email goes out async.
//
// runs once a day at 02:00 by default (cron in application.yml). i picked the
// middle of the night so it doesn't fight with normal traffic.
// TODO: like the outbox worker, this @Scheduled job would run on every instance
//       if i scale out. would need a lock (shedlock?) so only one runs it. for now
//       single instance, also gut.
@Component
public class DunningJob {

    private static final Logger log = LoggerFactory.getLogger(DunningJob.class);

    private final InvoiceRepository invoices;
    private final InvoiceTransition transition;
    private final OutboxPort outbox;

    public DunningJob(InvoiceRepository invoices, InvoiceTransition transition,
                      OutboxPort outbox) {
        this.invoices = invoices;
        this.transition = transition;
        this.outbox = outbox;
    }

    @Scheduled(cron = "${mahnflow.dunning.cron:0 0 2 * * *}")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now();
        markOverdue(today);
        advanceReminders();
    }

    private void markOverdue(LocalDate today) {
        for (Invoice invoice : invoices.findIssuedDueBefore(today)) {
            transition.check(invoice.status(), InvoiceEvent.MARK_OVERDUE);
            invoice.markOverdue();
            invoices.save(invoice);
            log.info("invoice {} is now overdue", invoice.id());
        }
    }

    private void advanceReminders() {
        for (Invoice invoice : invoices.findInDunningFlow()) {
            if (invoice.status() == InvoiceStatus.OVERDUE) {
                transition.check(invoice.status(), InvoiceEvent.SEND_FIRST_REMINDER);
                invoice.sendFirstReminder();
                invoices.save(invoice);
                outbox.append("DUNNING_LEVEL_1", invoice.id(),
                        "{\"invoiceId\":\"" + invoice.id() + "\"}");
            } else if (invoice.status() == InvoiceStatus.DUNNING_LEVEL_1) {
                transition.check(invoice.status(), InvoiceEvent.SEND_SECOND_REMINDER);
                invoice.sendSecondReminder();
                invoices.save(invoice);
                outbox.append("DUNNING_LEVEL_2", invoice.id(),
                        "{\"invoiceId\":\"" + invoice.id() + "\"}");
            }
        }
    }
}
