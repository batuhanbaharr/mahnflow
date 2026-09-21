package com.mahnflow.adapter.out.persistence.mapper;

import com.mahnflow.adapter.out.persistence.entity.InvoiceEntity;
import com.mahnflow.domain.invoice.Invoice;
import com.mahnflow.domain.money.Money;

import java.util.Currency;

// turns the domain object into a database row and back again.
public final class InvoiceMapper {

    private InvoiceMapper() {
    }

    public static InvoiceEntity toEntity(Invoice invoice) {
        return new InvoiceEntity(
                invoice.id(),
                invoice.customerId(),
                invoice.amount().amount(),
                invoice.amount().currency().getCurrencyCode(),
                invoice.dueDate(),
                invoice.status(),
                invoice.issuedAt(),
                invoice.paidAt(),
                invoice.version());
    }

    public static Invoice toDomain(InvoiceEntity entity) {
        Money money = new Money(entity.getAmount(),
                Currency.getInstance(entity.getCurrency()));
        return new Invoice(
                entity.getId(),
                entity.getCustomerId(),
                money,
                entity.getDueDate(),
                entity.getStatus(),
                entity.getIssuedAt(),
                entity.getPaidAt(),
                entity.getVersion());
    }
}
