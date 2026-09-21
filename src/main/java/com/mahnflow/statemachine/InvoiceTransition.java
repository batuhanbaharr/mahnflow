package com.mahnflow.statemachine;

import com.mahnflow.domain.invoice.InvalidStateChange;
import com.mahnflow.domain.invoice.InvoiceEvent;
import com.mahnflow.domain.invoice.InvoiceStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult.ResultType;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

// this is the bridge to spring statemachine.
// we build a fresh machine, set it to the invoice current state, fire the event
// and see if the machine accepts it. if not, we throw so the move never happens.
//
// the "reset the machine to a given state then send an event" trick is something
// i pieced together from the spring statemachine docs and a couple of stack
// overflow answers — the reactive api (startReactively / sendEvent returning a
// Flux) was confusing at first.
//
// TODO: i build a brand new machine on every single transition. it works, but i'm
//       not sure it's cheap under load. hoffe es ist schnell genug — should really
//       benchmark it with JMeter and maybe cache/pool the machines later.
@Component
public class InvoiceTransition {

    private final StateMachineFactory<InvoiceStatus, InvoiceEvent> factory;

    public InvoiceTransition(StateMachineFactory<InvoiceStatus, InvoiceEvent> factory) {
        this.factory = factory;
    }

    public void check(InvoiceStatus current, InvoiceEvent event) {
        StateMachine<InvoiceStatus, InvoiceEvent> machine = factory.getStateMachine();

        // put the machine into the state the invoice is in right now.
        machine.getStateMachineAccessor().doWithAllRegions(access ->
                access.resetStateMachineReactively(
                        new DefaultStateMachineContext<>(current, null, null, null)).block());
        machine.startReactively().block();

        var result = machine.sendEvent(
                        Mono.just(MessageBuilder.withPayload(event).build()))
                .blockLast();

        machine.stopReactively().block();

        boolean accepted = result != null && result.getResultType() == ResultType.ACCEPTED;
        if (!accepted) {
            throw new InvalidStateChange(current, event);
        }
    }
}
