package com.mahnflow.domain.customer;

import java.util.Objects;
import java.util.UUID;

// who we send the invoice to. kept very small on purpose.
public record Customer(UUID id, String name, String email) {

    public Customer {
        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(email, "email is null");
        if (!email.contains("@")) {
            throw new IllegalArgumentException("email looks wrong: " + email);
        }
    }
}
