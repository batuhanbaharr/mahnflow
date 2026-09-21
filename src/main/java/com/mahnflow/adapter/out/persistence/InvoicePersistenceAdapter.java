package com.mahnflow.adapter.out.persistence;

import com.mahnflow.adapter.out.persistence.mapper.InvoiceMapper;
import com.mahnflow.adapter.out.persistence.repo.InvoiceJpaRepository;
import com.mahnflow.application.port.out.InvoiceRepository;
import com.mahnflow.domain.invoice.Invoice;
import com.mahnflow.domain.invoice.InvoiceStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// the real database side of the InvoiceRepository port.
// it maps between the domain object and the jpa entity, so the rest of the app
// never sees jpa at all. it's a bit of extra mapping code (domain <-> entity),
// and honestly sometimes it feels like boilerplate, but it keeps the domain
// clean which was the goal.
@Component
public class InvoicePersistenceAdapter implements InvoiceRepository {

    private final InvoiceJpaRepository jpa;

    public InvoicePersistenceAdapter(InvoiceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Invoice save(Invoice invoice) {
        var saved = jpa.save(InvoiceMapper.toEntity(invoice));
        return InvoiceMapper.toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return jpa.findById(id).map(InvoiceMapper::toDomain);
    }

    @Override
    public List<Invoice> findAll() {
        return jpa.findAll().stream().map(InvoiceMapper::toDomain).toList();
    }

    @Override
    public List<Invoice> findIssuedDueBefore(LocalDate day) {
        return jpa.findByStatusAndDueDateBefore(InvoiceStatus.ISSUED, day).stream()
                .map(InvoiceMapper::toDomain).toList();
    }

    @Override
    public List<Invoice> findInDunningFlow() {
        var statuses = List.of(InvoiceStatus.OVERDUE,
                InvoiceStatus.DUNNING_LEVEL_1);
        return jpa.findByStatusIn(statuses).stream()
                .map(InvoiceMapper::toDomain).toList();
    }
}
