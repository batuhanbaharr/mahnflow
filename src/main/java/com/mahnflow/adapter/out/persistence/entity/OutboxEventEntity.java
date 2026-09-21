package com.mahnflow.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// one row = one job to do later (send email, build pdf...).
// it is written in the same transaction as the invoice change,
// so we never lose a job and never send one for a change that rolled back.
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(UUID id, String type, UUID invoiceId, String payload,
                             Instant createdAt) {
        this.id = id;
        this.type = type;
        this.invoiceId = invoiceId;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markProcessed() {
        this.processedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
