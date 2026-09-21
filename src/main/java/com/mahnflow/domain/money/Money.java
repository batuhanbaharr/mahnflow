package com.mahnflow.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

// small value object for money. amount and currency always travel together so i
// can never accidentally add euros to dollars. also using BigDecimal, not double
// — every "never use float for money" post on stack overflow scared me enough :)
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount is null");
        Objects.requireNonNull(currency, "currency is null");
        if (amount.scale() > 2) {
            amount = amount.setScale(2, RoundingMode.HALF_UP);
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("money can not be negative");
        }
    }

    public static Money euro(String value) {
        return new Money(new BigDecimal(value), Currency.getInstance("EUR"));
    }

    public static Money of(BigDecimal value, String currencyCode) {
        return new Money(value, Currency.getInstance(currencyCode));
    }

    public Money plus(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void checkSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency does not match: "
                    + this.currency + " vs " + other.currency);
        }
    }
}
