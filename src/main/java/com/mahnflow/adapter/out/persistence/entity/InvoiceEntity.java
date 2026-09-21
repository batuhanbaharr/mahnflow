package com.mahnflow.adapter.out.persistence.entity;

import com.mahnflow.domain.invoice.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// the database row for an invoice. this is the jpa side, kept apart from
// the domain class so the database does not leak into the business rules.
@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    // optimistic lock. if two requests update the same invoice at once, the
    // second one fails (OptimisticLockException) instead of quietly overwriting
    // the first. picked this over a pessimistic db lock after reading a baeldung
    // article — for invoices the conflict is rare, so optimistic is cheaper.
    // i map the exception to a 409 in ApiExceptionHandler so the client can retry.
    @Version
    private long version;

    protected InvoiceEntity() {
    }

    public InvoiceEntity(UUID id, UUID customerId, BigDecimal amount, String currency,
                         LocalDate dueDate, InvoiceStatus status, Instant issuedAt,
                         Instant paidAt, long version) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.dueDate = dueDate;
        this.status = status;
        this.issuedAt = issuedAt;
        this.paidAt = paidAt;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public long getVersion() {
        return version;
    }
}
