package com.casxduois.client.config;

public class NumberSetting extends Setting<Double> {
    private final double minimum;
    private final double maximum;
    private final double increment;

    public NumberSetting(String name, String vietnameseName, String description, double defaultValue, double minimum, double maximum, double increment) {
        super(name, vietnameseName, description, defaultValue);
        this.minimum = minimum;
        this.maximum = maximum;
        this.increment = increment;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public double getIncrement() {
        return increment;
    }

    public int getValueInt() {
        return (int) Math.round(this.value);
    }

    public float getValueFloat() {
        return this.value.floatValue();
    }

    @Override
    public void setValue(Double value) {
        if (value == null) return;
        double precision = (this.increment > 0.0) ? (1.0 / this.increment) : 10.0;
        double clamped = Math.max(this.minimum, Math.min(this.maximum, value));
        this.value = Math.round(clamped * precision) / precision;
    }

    /**
     * Cập nhật giá trị khi người dùng nhập trực tiếp từ bàn phím.
     * Cho phép nhập số chính xác không bị ép theo bước nhảy (increment) của thanh trượt.
     */
    public void setTypedValue(Double value) {
        if (value == null) return;
        double clamped = Math.max(this.minimum, Math.min(this.maximum, value));
        if (this.increment >= 1.0) {
            this.value = (double) Math.round(clamped);
        } else {
            double precision = (this.increment > 0.0) ? (1.0 / this.increment) : 10.0;
            this.value = Math.round(clamped * precision) / precision;
        }
    }
}
