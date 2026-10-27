package com.casxduois.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Trình quản lý tài khoản: Cho phép chuyển đổi tài khoản trực tiếp trong game không cần khởi động lại
 */
public class AccountManager {

    private final List<Account> accounts = new ArrayList<>();
    private Account currentAccount;
    private final File accountsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AccountManager(File clientDir) {
        this.accountsFile = new File(clientDir, "casxduois_accounts.json");
        loadAccounts();
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void addAccount(Account account) {
        // Tránh trùng username
        accounts.removeIf(acc -> acc.getUsername().equalsIgnoreCase(account.getUsername()));
        accounts.add(account);
        saveAccounts();
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        saveAccounts();
    }

    public boolean loginOffline(String username) {
        Account account = new Account(username);
        boolean success = login(account);
        if (success) {
            addAccount(account);
        }
        return success;
    }

    public boolean login(Account account) {
        try {
            Session newSession = new Session(account.getUsername(), account.getUuid(), account.getToken(), "mojang");
            setMinecraftSession(newSession);
            this.currentAccount = account;
            System.out.println("[CasxDuois] Đã đổi tài khoản thành công sang: " + account.getUsername());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setMinecraftSession(Session session) {
        Minecraft mc = Minecraft.getMinecraft();
        Class<?> mcClass = Minecraft.class;
        for (Field field : mcClass.getDeclaredFields()) {
            if (field.getType().equals(Session.class)) {
                try {
                    field.setAccessible(true);
                    field.set(mc, session);
                    return;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadAccounts() {
        if (!accountsFile.exists()) return;
        try (FileReader reader = new FileReader(accountsFile)) {
            List<Account> loaded = gson.fromJson(reader, new TypeToken<List<Account>>() {}.getType());
            if (loaded != null) {
                accounts.clear();
                accounts.addAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveAccounts() {
        try {
            if (!accountsFile.getParentFile().exists()) {
                accountsFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(accountsFile)) {
                gson.toJson(accounts, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
