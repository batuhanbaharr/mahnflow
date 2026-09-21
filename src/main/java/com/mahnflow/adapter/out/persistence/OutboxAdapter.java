package com.mahnflow.adapter.out.persistence;

import com.mahnflow.adapter.out.persistence.entity.OutboxEventEntity;
import com.mahnflow.adapter.out.persistence.repo.OutboxJpaRepository;
import com.mahnflow.application.port.out.OutboxPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// saves outbox jobs. because the service calls this inside its @Transactional
// method, the job row and the invoice change commit together or not at all.
@Component
public class OutboxAdapter implements OutboxPort {

    private final OutboxJpaRepository jpa;

    public OutboxAdapter(OutboxJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void append(String type, UUID invoiceId, String payloadJson) {
        var event = new OutboxEventEntity(UUID.randomUUID(), type, invoiceId,
                payloadJson, Instant.now());
        jpa.save(event);
    }
}
