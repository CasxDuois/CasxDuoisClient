package com.casxduois.client.event.events;

import com.casxduois.client.event.Event;
import net.minecraft.network.Packet;

public class EventPacket extends Event {
    private final Packet<?> packet;
    private final Direction direction;

    public enum Direction {
        INCOMING, OUTGOING
    }

    public EventPacket(Packet<?> packet, Direction direction) {
        this.packet = packet;
        this.direction = direction;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public Direction getDirection() {
        return direction;
    }
}
