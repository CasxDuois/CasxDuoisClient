package com.casxduois.client.event.events;

import com.casxduois.client.event.Event;

public class EventTick extends Event {
    private final Phase phase;

    public enum Phase {
        START, END
    }

    public EventTick(Phase phase) {
        this.phase = phase;
    }

    public Phase getPhase() {
        return phase;
    }
}
