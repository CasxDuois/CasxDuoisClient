package com.casxduois.client.event.events;

import com.casxduois.client.event.Event;

public class EventKey extends Event {
    private final int key;

    public EventKey(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }
}
