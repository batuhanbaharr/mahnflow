-- first schema. flyway runs this once and records it.

create table invoices (
    id          uuid primary key,
    customer_id uuid           not null,
    amount      numeric(12, 2) not null check (amount >= 0),
    currency    varchar(3)     not null,
    due_date    date           not null,
    status      varchar(32)    not null,
    issued_at   timestamptz,
    paid_at     timestamptz,
    version     bigint         not null default 0
);

create index idx_invoices_status on invoices (status);
create index idx_invoices_due_date on invoices (due_date);

-- outbox: one row per job to run later (send mail, build pdf...).
create table outbox_events (
    id           uuid primary key,
    type         varchar(64) not null,
    invoice_id   uuid        not null,
    payload      text        not null,
    created_at   timestamptz not null,
    processed_at timestamptz
);

-- partial index so the worker quickly finds jobs that are still open.
create index idx_outbox_open on outbox_events (created_at) where processed_at is null;
