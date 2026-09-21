package com.mahnflow.adapter.in.web;

import com.mahnflow.adapter.in.web.dto.CreateInvoiceRequest;
import com.mahnflow.adapter.in.web.dto.InvoiceResponse;
import com.mahnflow.application.port.in.InvoiceCommands.CreateInvoice;
import com.mahnflow.application.port.in.ManageInvoiceUseCase;
import com.mahnflow.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// the rest api. it only turns http into use case calls and back, no business
// rules live here. i tried to keep the controller "dumb" on purpose — read a few
// times that fat controllers are a code smell, so all the logic sits in the
// service and the domain.
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final ManageInvoiceUseCase useCase;
    private final IdempotencyService idempotency;

    public InvoiceController(ManageInvoiceUseCase useCase, IdempotencyService idempotency) {
        this.useCase = useCase;
        this.idempotency = idempotency;
    }

    // create a draft. accountants and admins may do this.
    // the Idempotency-Key header makes a retry safe.
    @PostMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
    public ResponseEntity<InvoiceResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody CreateInvoiceRequest request) {

        InvoiceResponse body = idempotency.runOnce(idemKey, InvoiceResponse.class, () -> {
            var command = new CreateInvoice(request.customerId(), request.amount(),
                    request.currency(), request.dueDate());
            return InvoiceResponse.from(useCase.createDraft(command));
        });
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
    public InvoiceResponse issue(@PathVariable UUID id) {
        return InvoiceResponse.from(useCase.issue(id));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
    public InvoiceResponse pay(@PathVariable UUID id) {
        return InvoiceResponse.from(useCase.pay(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceResponse cancel(@PathVariable UUID id) {
        return InvoiceResponse.from(useCase.cancel(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
    public InvoiceResponse getOne(@PathVariable UUID id) {
        return InvoiceResponse.from(useCase.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN')")
    public List<InvoiceResponse> getAll() {
        return useCase.getAll().stream().map(InvoiceResponse::from).toList();
    }
}
