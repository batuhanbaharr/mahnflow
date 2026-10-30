package com.mahnflow;

import com.mahnflow.adapter.out.email.EmailSender;
import com.mahnflow.adapter.out.persistence.entity.OutboxEventEntity;
import com.mahnflow.adapter.out.persistence.repo.OutboxJpaRepository;
import com.mahnflow.outbox.OutboxWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// two outbox workers run at the same moment, like two pods would.
// every job must be sent at most once.
@Import(OutboxWorkerConcurrencyTest.CountingEmailConfig.class)
@TestPropertySource(properties = "mahnflow.outbox.poll-ms=3600000")
class OutboxWorkerConcurrencyTest extends AbstractIntegrationTest {

    // how many times a mail was sent, per invoice id
    static final Map<UUID, AtomicInteger> SENT = new ConcurrentHashMap<>();

    @TestConfiguration
    static class CountingEmailConfig {
        @Bean
        @Primary
        EmailSender countingEmailSender() {
            return (type, invoiceId, payload) -> {
                SENT.computeIfAbsent(invoiceId, id -> new AtomicInteger()).incrementAndGet();
                // a real mail server is slow. this gives the race time to show up.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
        }
    }

    @Autowired
    OutboxWorker worker;

    @Autowired
    OutboxJpaRepository outbox;

    @Test
    void no_job_is_sent_twice_when_two_workers_run_at_once() throws Exception {
        // 1. put 20 open jobs into the outbox
        List<UUID> invoiceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            UUID invoiceId = UUID.randomUUID();
            invoiceIds.add(invoiceId);
            outbox.save(new OutboxEventEntity(UUID.randomUUID(), "TEST", invoiceId, "{}",
                    Instant.now()));
        }

        // 2. start two workers at the same moment
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        List<Future<?>> runs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            runs.add(pool.submit(() -> {
                ready.countDown();
                try {
                    ready.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                worker.process();
            }));
        }
        for (Future<?> run : runs) {
            run.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // 3. nobody got two mails, and the workers really did something
        // (a job can be missing if the app context of another test took it, that is fine)
        long sentAtLeastOnce = invoiceIds.stream().filter(SENT::containsKey).count();
        assertThat(sentAtLeastOnce).isPositive();
        for (UUID invoiceId : invoiceIds) {
            int times = SENT.containsKey(invoiceId) ? SENT.get(invoiceId).get() : 0;
            assertThat(times).as("mails sent for invoice %s", invoiceId).isLessThanOrEqualTo(1);
        }
    }
}