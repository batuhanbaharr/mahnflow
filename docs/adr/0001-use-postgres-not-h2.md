# adr 0001: use postgres instead of h2

status: accepted

## context
we need a database. h2 (in memory) is easy to start, but it is not what we
run in production. invoices are money, so the test must match real behaviour.

## decision
we use postgresql everywhere: local, tests (through testcontainers) and prod.
no h2 at all.

## why
- postgres has real types we rely on: `numeric` for money, `timestamptz`,
  proper `uuid`, and partial indexes for the outbox.
- "works on h2" often breaks on postgres (locking, sql dialect). i kept seeing
  this warning on stack overflow, so testing on the real engine felt safer.
- it just feels more honest to test against the same db i actually ship.

## trade-off
tests are a bit slower because docker has to start a container. we accept this
for the safety it gives. the container starts once and is shared.
