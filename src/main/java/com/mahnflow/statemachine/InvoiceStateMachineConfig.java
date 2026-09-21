package com.mahnflow.statemachine;

import com.mahnflow.domain.invoice.InvoiceEvent;
import com.mahnflow.domain.invoice.InvoiceStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

// here we draw the full map of allowed moves for an invoice.
// spring statemachine reads this map and refuses any move that is not drawn,
// so we never need long if-else chains to block wrong transitions.
//
// my first version of the whole app was a big pile of "if (status == ISSUED)"
// checks and it got messy really fast. found spring statemachine while googling
// "java state machine library", and drawing the moves once like this felt much
// cleaner. the builder api with all the .and() is a bit verbose though.
@Configuration
@EnableStateMachineFactory
public class InvoiceStateMachineConfig
        extends StateMachineConfigurerAdapter<InvoiceStatus, InvoiceEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<InvoiceStatus, InvoiceEvent> states)
            throws Exception {
        states
                .withStates()
                .initial(InvoiceStatus.DRAFT)
                .states(EnumSet.allOf(InvoiceStatus.class))
                .end(InvoiceStatus.PAID)
                .end(InvoiceStatus.CANCELLED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<InvoiceStatus, InvoiceEvent> transitions)
            throws Exception {
        transitions
                // normal path
                .withExternal()
                .source(InvoiceStatus.DRAFT).target(InvoiceStatus.ISSUED)
                .event(InvoiceEvent.ISSUE)
                .and()
                .withExternal()
                .source(InvoiceStatus.ISSUED).target(InvoiceStatus.OVERDUE)
                .event(InvoiceEvent.MARK_OVERDUE)
                .and()
                .withExternal()
                .source(InvoiceStatus.OVERDUE).target(InvoiceStatus.DUNNING_LEVEL_1)
                .event(InvoiceEvent.SEND_FIRST_REMINDER)
                .and()
                .withExternal()
                .source(InvoiceStatus.DUNNING_LEVEL_1).target(InvoiceStatus.DUNNING_LEVEL_2)
                .event(InvoiceEvent.SEND_SECOND_REMINDER)
                .and()
                // pay can happen from any open state
                .withExternal()
                .source(InvoiceStatus.ISSUED).target(InvoiceStatus.PAID)
                .event(InvoiceEvent.PAY)
                .and()
                .withExternal()
                .source(InvoiceStatus.OVERDUE).target(InvoiceStatus.PAID)
                .event(InvoiceEvent.PAY)
                .and()
                .withExternal()
                .source(InvoiceStatus.DUNNING_LEVEL_1).target(InvoiceStatus.PAID)
                .event(InvoiceEvent.PAY)
                .and()
                .withExternal()
                .source(InvoiceStatus.DUNNING_LEVEL_2).target(InvoiceStatus.PAID)
                .event(InvoiceEvent.PAY)
                .and()
                // cancel can happen from any open state
                .withExternal()
                .source(InvoiceStatus.DRAFT).target(InvoiceStatus.CANCELLED)
                .event(InvoiceEvent.CANCEL)
                .and()
                .withExternal()
                .source(InvoiceStatus.ISSUED).target(InvoiceStatus.CANCELLED)
                .event(InvoiceEvent.CANCEL)
                .and()
                .withExternal()
                .source(InvoiceStatus.OVERDUE).target(InvoiceStatus.CANCELLED)
                .event(InvoiceEvent.CANCEL)
                .and()
                .withExternal()
                .source(InvoiceStatus.DUNNING_LEVEL_1).target(InvoiceStatus.CANCELLED)
                .event(InvoiceEvent.CANCEL)
                .and()
                .withExternal()
                .source(InvoiceStatus.DUNNING_LEVEL_2).target(InvoiceStatus.CANCELLED)
                .event(InvoiceEvent.CANCEL);
    }
}
