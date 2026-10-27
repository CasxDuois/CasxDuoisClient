package com.casxduois.client.event.events;

import com.casxduois.client.event.Event;

/**
 * EventRender3D: Phát ra tại cuối chu kỳ vẽ 3D (renderWorldPass), trước khi vẽ HUD 2D
 * Dùng cho các module vẽ trong không gian 3D như ESP, Tracers, Box, Waypoints.
 */
public class EventRender3D extends Event {

    private final float partialTicks;

    public EventRender3D(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
