# adr 0002: db outbox pattern instead of a message broker

status: accepted

## context
when an invoice changes we must send an email / build a pdf. that work is slow
and talks to other systems. it must not block or break the invoice transaction.

## decision
we use the transactional outbox pattern. the service writes the invoice change
and an `outbox_events` row in the *same* db transaction. a scheduled worker
reads open rows and does the slow work, then marks them done.

## why
- one transaction means we can never have an invoice change with no job, or a
  job for a change that rolled back. they commit together or not at all.
- no extra broker (kafka, rabbitmq) to run, watch and learn for this size of app.
- at-least-once delivery is easy: the worker only marks a row done after the
  work succeeds, so a crash just retries it.

## trade-off
we lose the throughput and fan-out a real broker gives. for one service with
modest volume that is fine. if load grows we can move the outbox publisher to
push into kafka without changing the service code, because it only depends on
the `OutboxPort` interface.
