package com.casxduois.client.config;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, String vietnameseName, String description, boolean defaultValue) {
        super(name, vietnameseName, description, defaultValue);
    }

    public void toggle() {
        this.value = !this.value;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.value);
    }
}
