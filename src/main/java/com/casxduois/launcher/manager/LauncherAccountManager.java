package com.casxduois.launcher.manager;

import com.casxduois.launcher.model.LauncherAccount;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class LauncherAccountManager {

    private final List<LauncherAccount> accounts = new ArrayList<>();
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public LauncherAccountManager(File baseDir) {
        this.file = new File(baseDir, "casxduois_launcher_accounts.json");
        load();
        if (accounts.isEmpty()) {
            // Mẫu ban đầu
            accounts.add(new LauncherAccount("CasxDuoisPlayer"));
            save();
        }
    }

    public List<LauncherAccount> getAccounts() {
        return accounts;
    }

    public void addAccount(String username) {
        if (username == null || username.trim().isEmpty()) return;
        username = username.trim();
        for (LauncherAccount acc : accounts) {
            if (acc.getUsername().equalsIgnoreCase(username)) return;
        }
        accounts.add(new LauncherAccount(username));
        save();
    }

    public void batchCreateAccounts(String prefix, int count) {
        if (prefix == null || prefix.trim().isEmpty()) prefix = "BotAFK_";
        for (int i = 1; i <= count; i++) {
            String name = prefix + (i < 10 ? "0" + i : i);
            addAccount(name);
        }
        save();
    }

    public void removeAccount(LauncherAccount account) {
        accounts.remove(account);
        save();
    }

    public void load() {
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            List<LauncherAccount> loaded = gson.fromJson(reader, new TypeToken<List<LauncherAccount>>() {}.getType());
            if (loaded != null) {
                accounts.clear();
                accounts.addAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(accounts, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
