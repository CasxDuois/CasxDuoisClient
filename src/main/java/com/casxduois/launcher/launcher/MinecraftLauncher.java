package com.casxduois.launcher.launcher;

import com.casxduois.launcher.model.InstanceConfig;
import com.casxduois.launcher.model.LauncherAccount;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Trình dựng lệnh khởi chạy Minecraft 1.12.2 độc lập
 */
public class MinecraftLauncher {

    public static Process launchInstance(LauncherAccount account, InstanceConfig config) throws IOException {
        String javaExe = config.getJavaPath();
        File mcDir = new File(config.getMinecraftDir());
        File librariesDir = new File(mcDir, "libraries");
        File versionsDir = new File(mcDir, "versions");
        File version1122Dir = new File(versionsDir, "1.12.2");
        File versionJar = new File(version1122Dir, "1.12.2.jar");

        if (!versionJar.exists()) {
            throw new IOException("Không tìm thấy file 1.12.2.jar tại: " + versionJar.getAbsolutePath());
        }

        // Tạo chuỗi Classpath
        StringBuilder cpBuilder = new StringBuilder();
        addClasspathLibraries(librariesDir, cpBuilder);
        if (cpBuilder.length() > 0) {
            cpBuilder.append(File.pathSeparator);
        }
        cpBuilder.append(versionJar.getAbsolutePath());

        // Thư mục natives
        File nativesDir = new File(version1122Dir, "1.12.2-natives");
        if (!nativesDir.exists()) nativesDir.mkdirs();

        List<String> command = new ArrayList<>();
        command.add(javaExe);

        // RAM & JVM Arguments
        command.add("-Xms128M");
        command.add("-Xmx" + config.getRamPerTabMB() + "M");
        command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        command.add("-Dminecraft.launcher.brand=CasxDuoisLauncher");
        command.add("-Dminecraft.launcher.version=1.0-LITE");

        // JVM Garbage Collector tối ưu cho treo máy
        if (config.getCustomJvmArgs() != null && !config.getCustomJvmArgs().isEmpty()) {
            for (String arg : config.getCustomJvmArgs().split(" ")) {
                if (!arg.trim().isEmpty()) {
                    command.add(arg.trim());
                }
            }
        }

        // Classpath & Main class
        command.add("-cp");
        command.add(cpBuilder.toString());
        command.add("net.minecraft.client.main.Main");

        // Minecraft Game Arguments
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + account.getUsername()).getBytes()).toString();
        command.add("--username");
        command.add(account.getUsername());
        command.add("--version");
        command.add("1.12.2");
        command.add("--gameDir");
        command.add(mcDir.getAbsolutePath());
        command.add("--assetsDir");
        command.add(new File(mcDir, "assets").getAbsolutePath());
        command.add("--assetIndex");
        command.add("1.12");
        command.add("--uuid");
        command.add(uuid);
        command.add("--accessToken");
        command.add("0");
        command.add("--userType");
        command.add("legacy");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(mcDir);
        pb.redirectErrorStream(true);

        return pb.start();
    }

    private static void addClasspathLibraries(File dir, StringBuilder cp) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                addClasspathLibraries(f, cp);
            } else if (f.getName().endsWith(".jar") && !f.getName().contains("natives")) {
                if (cp.length() > 0) {
                    cp.append(File.pathSeparator);
                }
                cp.append(f.getAbsolutePath());
            }
        }
    }
}
