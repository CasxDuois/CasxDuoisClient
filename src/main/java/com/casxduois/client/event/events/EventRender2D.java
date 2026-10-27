package com.casxduois.client.event.events;

import com.casxduois.client.event.Event;

public class EventRender2D extends Event {
    private final float partialTicks;

    public EventRender2D(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
