package com.casxduois.client.util;

/**
 * Bộ đếm thời gian chính xác cao dùng cho các module AutoClicker, AutoEat, AutoReconnect, AntiAFK
 */
public class TimerUtils {
    private long lastMS = 0L;

    public TimerUtils() {
        reset();
    }

    public long getCurrentMS() {
        return System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastMS;
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - this.lastMS > time) {
            if (reset) {
                reset();
            }
            return true;
        }
        return false;
    }

    public boolean hasTimeElapsed(long time) {
        return hasTimeElapsed(time, false);
    }

    public void reset() {
        this.lastMS = System.currentTimeMillis();
    }

    public void setTime(long time) {
        this.lastMS = time;
    }
}
