package com.casxduois.client.config;

public abstract class Setting<T> {
    private final String name;
    private final String vietnameseName;
    private final String description;
    protected T value;

    public Setting(String name, String vietnameseName, String description, T defaultValue) {
        this.name = name;
        this.vietnameseName = vietnameseName;
        this.description = description;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
