package com.casxduois.launcher.model;

public class LauncherAccount {
    private String username;
    private boolean selected = true;
    private transient long processId = -1;
    private transient boolean running = false;

    public LauncherAccount(String username) {
        this.username = username;
        this.selected = true;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
