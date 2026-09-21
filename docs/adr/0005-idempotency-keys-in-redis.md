# adr 0005: idempotency keys kept in redis

status: accepted

## context
creating an invoice is a write. if the client times out and retries, we must
not create two invoices for one real request. money systems can not have doubles.

## decision
the client sends an `Idempotency-Key` header. we store the first response in
redis under that key with a 24h ttl. a repeat with the same key returns the
stored response instead of running again. we use `setIfAbsent` so two retries
arriving at the same moment can not both win.

## why
- redis is fast and has ttl built in, so old keys clean themselves up.
- the check sits at the edge (controller), the service stays simple.

## trade-off
redis is a second store to run. we already want it for distributed locks, so it
earns its place. if we needed the key history to survive a full redis loss we
could move it to a postgres table; we judged 24h in redis enough for retries.
