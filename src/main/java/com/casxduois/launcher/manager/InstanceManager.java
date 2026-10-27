package com.casxduois.launcher.manager;

import com.casxduois.launcher.launcher.MinecraftLauncher;
import com.casxduois.launcher.model.InstanceConfig;
import com.casxduois.launcher.model.LauncherAccount;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InstanceManager {

    private final Map<LauncherAccount, Process> runningProcesses = new ConcurrentHashMap<>();

    public void launchSingle(LauncherAccount account, InstanceConfig config, Consumer<String> logger) {
        new Thread(() -> {
            try {
                logger.accept("Đang khởi chạy tài khoản: " + account.getUsername() + " (RAM: " + config.getRamPerTabMB() + "MB)...");
                Process p = MinecraftLauncher.launchInstance(account, config);
                runningProcesses.put(account, p);
                account.setRunning(true);
                logger.accept("Đã mở thành công: " + account.getUsername());

                // Theo dõi tiến trình khi kết thúc
                p.waitFor();
                account.setRunning(false);
                runningProcesses.remove(account);
                logger.accept("Cửa sổ " + account.getUsername() + " đã đóng.");
            } catch (Exception e) {
                logger.accept("Lỗi khi mở " + account.getUsername() + ": " + e.getMessage());
            }
        }, "Launch-" + account.getUsername()).start();
    }

    public void launchBatch(java.util.List<LauncherAccount> accounts, InstanceConfig config, Consumer<String> logger) {
        new Thread(() -> {
            logger.accept("Bắt đầu mở " + accounts.size() + " tài khoản...");
            for (int i = 0; i < accounts.size(); i++) {
                LauncherAccount acc = accounts.get(i);
                launchSingle(acc, config, logger);
                try {
                    // Độ trễ 1.5 giây giữa các tab để không làm nghẽn CPU lúc khởi động
                    Thread.sleep(1500);
                } catch (InterruptedException ignored) {
                }
            }
            logger.accept("Đã mở xong " + accounts.size() + " tài khoản.");
        }, "Batch-Launcher").start();
    }

    public void killAll(Consumer<String> logger) {
        int count = runningProcesses.size();
        for (Map.Entry<LauncherAccount, Process> entry : runningProcesses.entrySet()) {
            try {
                entry.getValue().destroyForcibly();
                entry.getKey().setRunning(false);
            } catch (Exception ignored) {
            }
        }
        runningProcesses.clear();
        logger.accept("Đã đóng " + count + " cửa sổ đang chạy.");
    }

    public int getRunningCount() {
        return runningProcesses.size();
    }
}
