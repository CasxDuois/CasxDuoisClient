package com.casxduois.client.account;

import java.util.UUID;

public class Account {
    private String username;
    private String uuid;
    private String token;
    private AccountType type;

    public enum AccountType {
        OFFLINE("Cracked/Offline"),
        MOJANG("Mojang"),
        MICROSOFT("Microsoft");

        private final String displayName;

        AccountType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Account(String username) {
        this.username = username;
        this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString();
        this.token = "0";
        this.type = AccountType.OFFLINE;
    }

    public Account(String username, String uuid, String token, AccountType type) {
        this.username = username;
        this.uuid = uuid;
        this.token = token;
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUuid() {
        return uuid;
    }

    public String getToken() {
        return token;
    }

    public AccountType getType() {
        return type;
    }
}
