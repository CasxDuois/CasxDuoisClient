package com.casxduois.launcher.model;

public class InstanceConfig {
    private int ramPerTabMB = 1024;
    private String javaPath = detectDefaultJava();
    private String minecraftDir = System.getenv("APPDATA") + "\\.minecraft";

    private static String detectDefaultJava() {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            String portable = appData + "\\.minecraft\\runtime\\jre-x64\\bin\\javaw.exe";
            if (new java.io.File(portable).exists()) return portable;

            String tlauncher = appData + "\\.tlauncher\\legacy\\Minecraft\\jre\\jre-legacy\\windows-x64\\jre-legacy\\bin\\javaw.exe";
            if (new java.io.File(tlauncher).exists()) return tlauncher;

            String mcLegacy = appData + "\\.minecraft\\runtime\\jre-legacy\\windows-x64\\jre-legacy\\bin\\javaw.exe";
            if (new java.io.File(mcLegacy).exists()) return mcLegacy;
        }
        String[] baseDirs = {
            "C:\\Program Files\\Eclipse Adoptium",
            "C:\\Program Files (x86)\\Eclipse Adoptium",
            "C:\\Program Files\\Java",
            "C:\\Program Files (x86)\\Java",
            "C:\\Program Files\\BellSoft",
            "C:\\Program Files\\Zulu"
        };
        for (String base : baseDirs) {
            java.io.File dir = new java.io.File(base);
            if (dir.exists() && dir.isDirectory()) {
                java.io.File[] subs = dir.listFiles();
                if (subs != null) {
                    for (java.io.File sub : subs) {
                        java.io.File jw = new java.io.File(sub, "bin\\javaw.exe");
                        if (jw.exists()) return jw.getAbsolutePath();
                    }
                }
            }
        }
        return "javaw.exe";
    }
    private String customJvmArgs = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dsun.java2d.opengl=true -Dfml.splash=false";

    public int getRamPerTabMB() {
        return ramPerTabMB;
    }

    public void setRamPerTabMB(int ramPerTabMB) {
        this.ramPerTabMB = ramPerTabMB;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public String getMinecraftDir() {
        return minecraftDir;
    }

    public void setMinecraftDir(String minecraftDir) {
        this.minecraftDir = minecraftDir;
    }

    public String getCustomJvmArgs() {
        return customJvmArgs;
    }

    public void setCustomJvmArgs(String customJvmArgs) {
        this.customJvmArgs = customJvmArgs;
    }
}
