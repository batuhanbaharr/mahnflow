# mahnflow

A small B2B backend for invoicing and automatic dunning (German: *Mahnwesen*).
Backend only, no frontend, you interact with it via curl / Postman / Swagger.

> Learning / portfolio project. I wanted something closer to real B2B software
> than another todo-list CRUD app, so I picked a topic that basically every SaaS
> company in Germany has to solve: billing and chasing late payments. I'm still
> a junior, so there are rough edges, and I list them openly in
> **Bekannte Probleme / TODOs** at the bottom instead of hiding them.

---

## Why this project

I wanted to work as a backend developer in Germany. Reading job ads, I kept running
into two terms I never learned at university: **Mahnwesen** (the legal-ish,
escalating process of reminding a customer that an invoice is overdue) and
**GoBD** (German bookkeeping and tax rules).

So instead of another CRUD demo I built a small but *correct* version of that.
The interesting part isn't the REST endpoints, it's the boring-but-hard stuff
underneath: not creating an invoice twice on a retry, never allowing an illegal
state change, and sending mails without blocking the request. Those are the
things I wanted to learn by actually building them, not just reading about them.

---

## What it does

- Create an invoice (`DRAFT`), issue it, take payment, or cancel it.
- A scheduled job marks invoices `OVERDUE` after the due date passes.
- The same job pushes overdue invoices to reminder level 1, then level 2.
- Each reminder becomes an email job that is sent asynchronously.

Every state change goes through a state machine, so an illegal move (paying a
draft, un-paying an invoice) is simply rejected.

---

## Concepts I focused on

| Topic | What I wanted to learn | Where |
|------|------------------------|-------|
| **Idempotency** | a retried "create" must not produce two invoices | `idempotency/IdempotencyService.java` |
| **State machine** | model the invoice life cycle so illegal moves are impossible | `statemachine/InvoiceStateMachineConfig.java` |
| **Outbox pattern** | send mails async without losing them or blocking the transaction | `outbox/OutboxWorker.java` |
| **Optimistic locking** | two racing updates, one fails instead of silently overwriting | `@Version` in `InvoiceEntity.java` |
| **Hexagonal layout** | keep the domain free of Spring imports | `domain/` package |
| **Testcontainers** | test against real Postgres + Redis, not H2 | `AbstractIntegrationTest.java` |

I don't claim these are perfect, but I can explain *why* each one is there, which
was the whole point.

---

## Architecture

Hexagonal / clean-ish layout: the domain in the middle knows nothing about
Spring, Postgres or HTTP, and everything points inward. Probably not 100%
textbook, but close enough for a proof of concept.

```mermaid
flowchart TB
    subgraph in[inbound]
        C[rest controller]
    end
    subgraph app[application]
        UC[InvoiceService\nuse case]
        P1[(InvoiceRepository port)]
        P2[(OutboxPort)]
    end
    subgraph dom[domain - no spring]
        INV[Invoice + rules]
        SM[state machine]
    end
    subgraph out[outbound]
        DB[(postgres)]
        RDS[(redis)]
        MAIL[email sender]
    end

    C --> UC
    UC --> INV
    UC --> SM
    UC --> P1 --> DB
    UC --> P2 --> DB
    C -.idempotency.-> RDS
    OW[outbox worker] --> DB
    OW --> MAIL
```

### Invoice life cycle (the state machine)

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> ISSUED: issue
    DRAFT --> CANCELLED: cancel
    ISSUED --> PAID: pay
    ISSUED --> OVERDUE: mark overdue (job)
    ISSUED --> CANCELLED: cancel
    OVERDUE --> DUNNING_LEVEL_1: first reminder (job)
    OVERDUE --> PAID: pay
    DUNNING_LEVEL_1 --> DUNNING_LEVEL_2: second reminder (job)
    DUNNING_LEVEL_1 --> PAID: pay
    DUNNING_LEVEL_2 --> PAID: pay
    PAID --> [*]
    CANCELLED --> [*]
```

---

## Why these tech choices

I didn't pick these because they're fancy. I picked them because they fit the
problem and I wanted to learn them. The longer versions are in
[`docs/adr`](docs/adr).

- **Postgres, not H2 (even in tests)** — "works on H2, breaks on Postgres" is a
  classic trap (different SQL, different locking). Since this is about money, I
  want the tests to run against the real database, so I use Testcontainers.
  Slower tests, but honest ones.
- **State machine instead of if-else** — my first version was a growing pile of
  `if (status == ...)` and got ugly fast. With Spring StateMachine I define the
  allowed moves once, and an illegal move is rejected automatically.
- **Outbox pattern instead of Kafka/RabbitMQ** — for a single service a message
  broker felt like overkill (one more thing to run and understand). The
  transactional outbox gives the same "never lose a message" guarantee with just
  a database table.
- **Redis for idempotency keys** — I needed a fast store with a TTL so old keys
  expire on their own.
- **JWT with a shared secret** — a proper setup would use Keycloak, but running a
  full identity provider just to demo the API felt heavy. For now I sign tokens
  with one secret. Dev only, not production. See ADR 0004.

---

## Tech stack

- Java 21, Spring Boot **3.3.x**
- Spring Web (blocking MVC — for this CRUD-ish workload WebFlux would just add
  complexity I don't need yet)
- PostgreSQL + Flyway 10 + Spring Data JPA
- Redis (idempotency keys)
- Spring StateMachine 3.2.x
- Spring Security + OAuth2 Resource Server (JWT)
- Micrometer + Prometheus + Grafana
- Testcontainers + JUnit 5
- GitHub Actions (build, SpotBugs report, JaCoCo coverage)

---

## How to run

You only need Docker.

```bash
docker-compose up
```

This starts the app (`:8080`), Postgres, Redis, Prometheus (`:9090`) and Grafana
(`:3000`). Flyway builds the schema on first start.

Without Docker (needs Java 21 + a local Postgres + Redis):

```bash
mvn spring-boot:run
```

### Try the API

Get a demo token first (this `/dev/token` endpoint is dev only, off in prod):

```bash
TOKEN=$(curl -s "http://localhost:8080/dev/token?role=ACCOUNTANT" | jq -r .token)
```

Create a draft invoice (note the idempotency key):

```bash
curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"11111111-1111-1111-1111-111111111111",
       "amount":250.00,"currency":"EUR","dueDate":"2026-12-31"}'
```

Issue + pay it (use the id from the response):

```bash
curl -X POST http://localhost:8080/api/invoices/<id>/issue -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8080/api/invoices/<id>/pay   -H "Authorization: Bearer $TOKEN"
```

Try a wrong move to see the state machine block it (paying a fresh draft returns
`409 Conflict`):

```bash
curl -i -X POST http://localhost:8080/api/invoices/<new-draft-id>/pay \
  -H "Authorization: Bearer $TOKEN"
```

- Swagger UI: http://localhost:8080/swagger-ui.html (click **Authorize**, paste the token)
- OpenAPI JSON: http://localhost:8080/v3/api-docs

---

## Bekannte Probleme / TODOs (honest list)

I'd rather list these myself than have an interviewer find them. These are the
things I know aren't great yet:

- ⚠️ **Idempotency has a small race window.** I run the action first and only
  then do `setIfAbsent` on the Redis key. Two identical requests in the same
  millisecond can both run before the key is written. The proper fix is to
  reserve the key *first* (a "pending" marker) and let the second one wait.
  On the todo list.
- ⚠️ **The outbox worker isn't safe for multiple app instances.** Two instances
  could pick up the same outbox rows. The fix is `SELECT ... FOR UPDATE SKIP
  LOCKED`; I haven't wired it into Spring Data yet. Fine for a single instance.
- **The state machine builds a fresh machine on every transition.** Works, but I
  haven't load-tested it yet (JMeter is on the list).
- **No real emails.** `LogEmailSender` just logs. Plugging in a real provider
  (or a PDF generator) is the next step.
- **`Customer` is a domain class without a table yet.** I modeled it but didn't
  build the customer CRUD, so for now invoices just carry a `customerId`.
- **Security uses a shared HMAC secret.** Fine for the demo, not for production;
  would move to Keycloak / a real issuer-uri.
- **GoBD compliance is only partial.** Real bookkeeping rules want an immutable
  audit log of every change. I don't have that yet.

If you spot more, please tell me, I'm here to learn.

---

## Testing

```bash
mvn verify
```

- `InvoiceDomainTest` — fast unit tests for the domain rules (no Spring).
- `InvoiceApiIntegrationTest` — full HTTP flow against **real** Postgres + Redis
  via Testcontainers. Needs Docker running.

Coverage report ends up in `target/site/jacoco/index.html`.

---

## Project layout

```
domain/        pure business rules, no spring
application/   use cases + ports (interfaces)
adapter/in/    rest controllers + dto
adapter/out/   postgres, email
statemachine/  invoice state config + transition check
idempotency/   redis based de-dup
outbox/        background worker for async jobs
dunning/       scheduled mahnwesen job
security/      jwt resource server
```
