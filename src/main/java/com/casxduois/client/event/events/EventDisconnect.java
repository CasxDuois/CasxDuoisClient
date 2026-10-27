package com.casxduois.client.event.events;

import com.casxduois.client.event.Event;
import net.minecraft.util.text.ITextComponent;

public class EventDisconnect extends Event {
    private final ITextComponent reason;

    public EventDisconnect(ITextComponent reason) {
        this.reason = reason;
    }

    public ITextComponent getReason() {
        return reason;
    }
}
