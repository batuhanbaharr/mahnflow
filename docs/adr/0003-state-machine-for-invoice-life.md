# adr 0003: spring statemachine for the invoice life cycle

status: accepted

## context
an invoice moves through many states: draft, issued, paid, overdue, dunning 1,
dunning 2, cancelled. some moves are allowed, many are not (you can not pay a
draft, you can not un-pay an invoice). doing this with if-else grows messy and
easy to get wrong.

## decision
we draw the whole allowed map in `InvoiceStateMachineConfig` and let spring
statemachine accept or reject each move. the domain object also guards itself
as a second safety net.

## why
- the allowed moves live in one place and read like a diagram.
- a move that is not drawn is rejected automatically, no extra code.
- easy to show in the readme as a picture, which helps reviewers.

## trade-off
there is some double checking: both the state machine and the domain object
block bad moves. we keep both on purpose. the state machine is the readable map;
the domain guard makes sure the object stays correct even if some future caller
forgets to ask the state machine first (defense in depth).
