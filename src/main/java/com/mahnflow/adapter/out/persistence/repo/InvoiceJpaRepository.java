package com.mahnflow.adapter.out.persistence.repo;

import com.mahnflow.adapter.out.persistence.entity.InvoiceEntity;
import com.mahnflow.domain.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// spring data gives us the basic queries for free.
// we only add the two finders the dunning job needs.
public interface InvoiceJpaRepository extends JpaRepository<InvoiceEntity, UUID> {

    List<InvoiceEntity> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate day);

    List<InvoiceEntity> findByStatusIn(List<InvoiceStatus> statuses);
}
