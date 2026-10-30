package com.mahnflow.outbox;

import com.mahnflow.adapter.out.email.EmailSender;
import com.mahnflow.adapter.out.persistence.entity.OutboxEventEntity;
import com.mahnflow.adapter.out.persistence.repo.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// the worker side of the outbox pattern.
// every few seconds it reads jobs that are not done, does the work
// (here: send mail) and marks them done. this runs apart from the main
// request, so a slow mail server never blocks an invoice update.
//
// rows are locked with SKIP LOCKED, so several instances can run this safely.
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int BATCH = 50;

    private final OutboxJpaRepository outbox;
    private final EmailSender emailSender;

    public OutboxWorker(OutboxJpaRepository outbox, EmailSender emailSender) {
        this.outbox = outbox;
        this.emailSender = emailSender;
    }

    @Scheduled(fixedDelayString = "${mahnflow.outbox.poll-ms:3000}")
    @Transactional
    public void process() {
        List<OutboxEventEntity> jobs = outbox.lockNextBatch(PageRequest.of(0, BATCH));
        for (OutboxEventEntity job : jobs) {
            emailSender.send(job.getType(), job.getInvoiceId(), job.getPayload());
            job.markProcessed();
        }
        if (!jobs.isEmpty()) {
            log.info("processed {} outbox jobs", jobs.size());
        }
    }
}
