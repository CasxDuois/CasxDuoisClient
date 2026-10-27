package com.casxduois.client.core;

import com.casxduois.client.util.TimerUtils;

/**
 * Bộ tối ưu hóa RAM: Thu gom rác nhẹ nhàng và giải phóng bộ nhớ đệm
 */
public class MemoryOptimizer {

    private final TimerUtils timer = new TimerUtils();
    private long lastUsedMemory = 0;
    private long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);

    public long getUsedMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
    }

    public long getMaxMemoryMB() {
        return maxMemory;
    }

    public int getMemoryPercent() {
        long max = getMaxMemoryMB();
        if (max <= 0) return 0;
        return (int) ((getUsedMemoryMB() * 100) / max);
    }

    public void tick() {
        // Tự động dọn dẹp bộ nhớ mỗi 60 giây nếu lượng RAM sử dụng vượt quá 70%
        if (timer.hasTimeElapsed(60000, true)) {
            if (getMemoryPercent() > 70) {
                cleanMemoryAsync();
            }
        }
    }

    public void cleanMemoryAsync() {
        new Thread(() -> {
            try {
                System.gc();
            } catch (Exception ignored) {
            }
        }, "CasxDuois-Memory-Cleaner").start();
    }
}
