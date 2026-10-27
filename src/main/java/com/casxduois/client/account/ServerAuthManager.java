package com.casxduois.client.account;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.modules.afk.AutoReconnectModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * ServerAuthManager: Quản lý và bảo vệ mật khẩu tự động đăng nhập (/login) theo từng server và tài khoản cụ thể.
 * TỐI ƯU CƠ CHẾ LƯU MẬT KHẨU:
 * 1. Mã hóa bảo mật AES-128-CBC (với khóa định danh thiết bị độc quyền, không lưu plain-text thô).
 * 2. Ghi tệp nguyên tử (Atomic Write qua file .tmp và sao lưu .bak, chống hỏng file khi crash/mất điện).
 * 3. Tương thích ngược 100% với mật khẩu lưu dạng cũ (tự động nâng cấp mã hóa khi lưu).
 * 4. So khớp Domain/Subdomain chuẩn mực (chống rò rỉ mật khẩu giữa các server khác nhau).
 * 5. Chỉ lưu mật khẩu khi máy chủ đã đồng ý xác nhận mật khẩu chính xác.
 */
public class ServerAuthManager {

    /**
     * Bộ mã hóa AES-128-CBC bảo vệ mật khẩu offline trên máy tính người chơi
     */
    private static class CryptoHelper {
        private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
        private final byte[] keyBytes;

        public CryptoHelper(File baseDir) {
            this.keyBytes = deriveKey(baseDir);
        }

        private byte[] deriveKey(File baseDir) {
            try {
                if (!baseDir.exists()) {
                    baseDir.mkdirs();
                }
                File saltFile = new File(baseDir, ".client_auth_salt");
                String salt;
                if (saltFile.exists() && saltFile.length() > 0) {
                    byte[] raw = Files.readAllBytes(saltFile.toPath());
                    salt = new String(raw, StandardCharsets.UTF_8).trim();
                } else {
                    salt = UUID.randomUUID().toString().replace("-", "") + System.getProperty("user.name", "casxduois");
                    try {
                        Files.write(saltFile.toPath(), salt.getBytes(StandardCharsets.UTF_8));
                    } catch (Throwable ignored) {}
                }

                String seed = "CasxDuois-Auth-V2-" + salt + "-" + System.getProperty("user.name", "user");
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] hash = sha.digest(seed.getBytes(StandardCharsets.UTF_8));
                byte[] key = new byte[16]; // 128-bit AES key
                System.arraycopy(hash, 0, key, 0, 16);
                return key;
            } catch (Throwable t) {
                // Khóa dự phòng tiêu chuẩn nếu môi trường không cho phép ghi salt
                return new byte[]{ (byte)0x4C, (byte)0xA7, (byte)0x1B, (byte)0x93, (byte)0xE5, (byte)0x2D, (byte)0x8F, (byte)0x64,
                                   (byte)0x39, (byte)0x72, (byte)0xBD, (byte)0x08, (byte)0xFA, (byte)0x61, (byte)0x9E, (byte)0x50 };
            }
        }

        public String encrypt(String plainText) {
            if (plainText == null || plainText.isEmpty()) return plainText;
            try {
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
                byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

                // Ghép [16 bytes IV] + [Ciphertext] -> Base64
                byte[] combined = new byte[iv.length + encrypted.length];
                System.arraycopy(iv, 0, combined, 0, iv.length);
                System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

                return "ENC:" + Base64.getEncoder().encodeToString(combined);
            } catch (Throwable t) {
                CasxDuoisClient.LOGGER.error("[ServerAuth] Lỗi khi mã hóa mật khẩu: " + t.getMessage());
                return plainText;
            }
        }

        public String decrypt(String cipherText) {
            if (cipherText == null || cipherText.isEmpty()) return cipherText;
            // Nếu không có tiền tố ENC: thì đây là dữ liệu phiên bản cũ chưa mã hóa -> Trả về nguyên bản
            if (!cipherText.startsWith("ENC:")) {
                return cipherText;
            }

            try {
                byte[] combined = Base64.getDecoder().decode(cipherText.substring(4));
                if (combined.length <= 16) return "";

                byte[] iv = new byte[16];
                System.arraycopy(combined, 0, iv, 0, 16);
                int encLength = combined.length - 16;
                byte[] encrypted = new byte[encLength];
                System.arraycopy(combined, 16, encrypted, 0, encLength);

                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
                byte[] decrypted = cipher.doFinal(encrypted);

                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (Throwable t) {
                CasxDuoisClient.LOGGER.error("[ServerAuth] Lỗi khi giải mã mật khẩu: " + t.getMessage());
                return null;
            }
        }
    }

    public static class PendingAuth {
        private final String username;
        private final String serverIP;
        private final String password;
        private final long timestamp;
        private final boolean autoLogin;
        private final String commandType; // "LOGIN", "REGISTER", "CHANGE_PASSWORD"

        public PendingAuth(String username, String serverIP, String password, boolean autoLogin, String commandType) {
            this.username = username;
            this.serverIP = serverIP;
            this.password = password;
            this.timestamp = System.currentTimeMillis();
            this.autoLogin = autoLogin;
            this.commandType = commandType;
        }

        public String getUsername() {
            return username;
        }

        public String getServerIP() {
            return serverIP;
        }

        public String getPassword() {
            return password;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isAutoLogin() {
            return autoLogin;
        }

        public String getCommandType() {
            return commandType;
        }

        public boolean isExpired(long maxAgeMs) {
            return (System.currentTimeMillis() - timestamp) > maxAgeMs;
        }
    }

    private static final String[] FAILURE_PATTERNS = {
        "sai mat khau",
        "mat khau khong chinh xac",
        "mat khau khong dung",
        "mat khau sai",
        "dang nhap that bai",
        "mat khau khong khop",
        "mat khau qua ngan",
        "mat khau khong hop le",
        "vui long nhap lai mat khau",
        "tai khoan nay da duoc dang ky",
        "wrong password",
        "incorrect password",
        "password incorrect",
        "password is incorrect",
        "invalid password",
        "login failed",
        "failed login",
        "passwords do not match",
        "password mismatch",
        "authentication failed"
    };

    private static final String[] SUCCESS_PATTERNS = {
        "dang nhap thanh cong",
        "da dang nhap thanh cong",
        "ban da dang nhap thanh cong",
        "da dang nhap roi",
        "ban da dang nhap roi",
        "ban da dang nhap",
        "dang ky thanh cong",
        "da dang ky thanh cong",
        "ban da dang ky thanh cong",
        "xac thuc thanh cong",
        "da xac thuc thanh cong",
        "mat khau chinh xac",
        "chao mung tro lai",
        "dang nhap hoan tat",
        "doi mat khau thanh cong",
        "mat khau da duoc thay doi",
        "successful login",
        "successfully logged in",
        "login successful",
        "login successfully",
        "logged in successfully",
        "you are now logged in",
        "you have successfully logged in",
        "you are logged in",
        "successful registration",
        "successfully registered",
        "registered successfully",
        "you are now registered",
        "registration successful",
        "authentication successful",
        "authenticated successfully",
        "you have been authenticated",
        "password accepted",
        "password changed successfully",
        "successfully changed password",
        "welcome back"
    };

    private final File authFile;
    private final File backupFile;
    private final File tempFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final CryptoHelper crypto;

    // Map<Username, Map<ServerIP, Password>> (Lưu trữ mật khẩu đã giải mã trong RAM cho hiệu năng cao)
    private final Map<String, Map<String, String>> credentials = new HashMap<>();

    // Trạng thái chờ Server đồng ý/xác nhận mật khẩu
    private volatile PendingAuth pendingAuth = null;

    public ServerAuthManager(File baseDir) {
        this.authFile = new File(baseDir, "server_auth.json");
        this.backupFile = new File(baseDir, "server_auth.json.bak");
        this.tempFile = new File(baseDir, "server_auth.json.tmp");
        this.crypto = new CryptoHelper(baseDir);
        load();
    }

    /**
     * Nạp dữ liệu mật khẩu từ tệp tin, tự động khôi phục từ bản sao lưu .bak nếu file chính bị lỗi/hỏng
     */
    public synchronized void load() {
        credentials.clear();

        File target = authFile;
        if (!target.exists() || target.length() == 0) {
            if (backupFile.exists() && backupFile.length() > 0) {
                target = backupFile;
                CasxDuoisClient.LOGGER.warn("[ServerAuth] File chính không khả dụng, đang khôi phục từ backup: " + backupFile.getName());
            } else {
                save();
                return;
            }
        }

        boolean success = parseAuthFile(target);
        if (!success && target != backupFile && backupFile.exists() && backupFile.length() > 0) {
            CasxDuoisClient.LOGGER.warn("[ServerAuth] Lỗi đọc file chính, đang tự động khôi phục từ file backup .bak...");
            success = parseAuthFile(backupFile);
            if (success) {
                save(); // Đồng bộ lại file chính
            }
        }
    }

    private boolean parseAuthFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonElement rootElem = new JsonParser().parse(reader);
            if (!rootElem.isJsonObject()) return false;
            JsonObject root = rootElem.getAsJsonObject();

            for (Map.Entry<String, JsonElement> userEntry : root.entrySet()) {
                String username = userEntry.getKey();
                if (username.equals("*") || username.equalsIgnoreCase("BotAFK_01")) continue;

                if (userEntry.getValue().isJsonObject()) {
                    JsonObject serverObj = userEntry.getValue().getAsJsonObject();
                    Map<String, String> serverMap = new HashMap<>();

                    for (Map.Entry<String, JsonElement> sEntry : serverObj.entrySet()) {
                        String rawIP = sEntry.getKey();
                        if (rawIP.equals("*")) continue;
                        String cleanIP = cleanServerIP(rawIP);
                        if (cleanIP.isEmpty()) continue;

                        String passVal = null;
                        if (sEntry.getValue().isJsonPrimitive()) {
                            passVal = sEntry.getValue().getAsString();
                        } else if (sEntry.getValue().isJsonObject()) {
                            JsonObject entryObj = sEntry.getValue().getAsJsonObject();
                            if (entryObj.has("password") && entryObj.get("password").isJsonPrimitive()) {
                                passVal = entryObj.get("password").getAsString();
                            }
                        }

                        if (passVal != null && !passVal.trim().isEmpty()) {
                            String decrypted = crypto.decrypt(passVal);
                            if (decrypted != null && !decrypted.trim().isEmpty()) {
                                serverMap.put(cleanIP, decrypted.trim());
                            }
                        }
                    }

                    if (!serverMap.isEmpty()) {
                        credentials.put(username, serverMap);
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            CasxDuoisClient.LOGGER.error("[ServerAuth] Lỗi khi phân tích tệp " + file.getName() + ": " + t.getMessage());
            return false;
        }
    }

    /**
     * Ghi tệp nguyên tử (Atomic Write): Mã hóa tất cả mật khẩu, ghi ra file .tmp trước, backup .bak và move nguyên tử.
     */
    public synchronized void save() {
        try {
            File parentDir = authFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            JsonObject root = new JsonObject();
            for (Map.Entry<String, Map<String, String>> userEntry : credentials.entrySet()) {
                if (userEntry.getKey().equals("*")) continue;
                JsonObject serverObj = new JsonObject();

                for (Map.Entry<String, String> sEntry : userEntry.getValue().entrySet()) {
                    if (sEntry.getKey().equals("*")) continue;
                    String cleanIP = cleanServerIP(sEntry.getKey());
                    String plainPass = sEntry.getValue();

                    if (!cleanIP.isEmpty() && plainPass != null && !plainPass.trim().isEmpty()) {
                        // MÃ HÓA MẬT KHẨU TRƯỚC KHI LƯU VÀO ĐĨA
                        String encrypted = crypto.encrypt(plainPass.trim());
                        serverObj.addProperty(cleanIP, encrypted);
                    }
                }

                if (serverObj.size() > 0) {
                    root.add(userEntry.getKey(), serverObj);
                }
            }

            // 1. Ghi dữ liệu vào file tạm thời
            try (FileWriter writer = new FileWriter(tempFile)) {
                gson.toJson(root, writer);
                writer.flush();
            }

            // 2. Tạo file backup nếu file chính đang có dữ liệu hợp lệ
            if (authFile.exists() && authFile.length() > 0) {
                try {
                    Files.copy(authFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable ignored) {}
            }

            // 3. Di chuyển nguyên tử (Atomic Replace) sang file chính
            try {
                Files.move(tempFile.toPath(), authFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Throwable fallback) {
                Files.move(tempFile.toPath(), authFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (Throwable t) {
            CasxDuoisClient.LOGGER.error("[ServerAuth] Lỗi khi ghi tệp server_auth.json: " + t.getMessage(), t);
        }
    }

    /**
     * Tra cứu mật khẩu theo Username và Server IP với cơ chế so khớp domain an toàn
     */
    public synchronized String getPassword(String username, String serverIP) {
        if (username == null || serverIP == null) return null;

        String cleanIP = cleanServerIP(serverIP);
        if (cleanIP.isEmpty()) return null;

        // 1. Tìm map theo tên acc cụ thể (không phân biệt hoa/thường)
        Map<String, String> userMap = findUserMap(username);
        if (userMap == null || userMap.isEmpty()) return null;

        // 2. Khớp chính xác IP/Host
        if (userMap.containsKey(cleanIP)) {
            return userMap.get(cleanIP);
        }

        // 3. Khớp thông minh Domain / Subdomain an toàn (không rò rỉ sang TLD hay server lạ)
        for (Map.Entry<String, String> sEntry : userMap.entrySet()) {
            String savedIP = sEntry.getKey();
            if (isSameServerDomain(cleanIP, savedIP)) {
                return sEntry.getValue();
            }
        }

        return null;
    }

    public synchronized void setPassword(String username, String serverIP, String password) {
        if (username == null || serverIP == null) return;
        String cleanIP = cleanServerIP(serverIP);
        if (cleanIP.isEmpty()) return;

        // Tìm key của username đã tồn tại (không phân biệt hoa/thường) để tránh nhân đôi tài khoản
        String targetKey = null;
        for (String u : credentials.keySet()) {
            if (u.equalsIgnoreCase(username)) {
                targetKey = u;
                break;
            }
        }
        if (targetKey == null) {
            targetKey = username;
        }

        if (password == null || password.trim().isEmpty()) {
            Map<String, String> userMap = credentials.get(targetKey);
            if (userMap != null) {
                userMap.remove(cleanIP);
                userMap.entrySet().removeIf(e -> isSameServerDomain(cleanIP, e.getKey()));
                if (userMap.isEmpty()) {
                    credentials.remove(targetKey);
                }
            }
        } else {
            credentials.computeIfAbsent(targetKey, k -> new HashMap<>()).put(cleanIP, password.trim());
        }

        save();
    }

    public synchronized boolean hasPassword(String username, String serverIP) {
        return getPassword(username, serverIP) != null;
    }

    private Map<String, String> findUserMap(String username) {
        if (username == null) return null;
        for (Map.Entry<String, Map<String, String>> entry : credentials.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(username)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Chuẩn hóa Server IP: loại bỏ port và khoảng trắng thừa
     */
    public String normalizeServerIP(String ip) {
        return cleanServerIP(ip);
    }

    public static String cleanServerIP(String ip) {
        if (ip == null) return "";
        ip = ip.trim().toLowerCase();
        int colonIdx = ip.indexOf(':');
        if (colonIdx > 0) {
            ip = ip.substring(0, colonIdx);
        }
        return ip;
    }

    /**
     * So khớp hai tên miền hoặc IP server một cách an toàn và thông minh
     */
    public static boolean isSameServerDomain(String ip1, String ip2) {
        if (ip1 == null || ip2 == null) return false;
        ip1 = cleanServerIP(ip1);
        ip2 = cleanServerIP(ip2);

        if (ip1.isEmpty() || ip2.isEmpty()) return false;
        if (ip1.equalsIgnoreCase(ip2)) return true;

        // Nếu là IPv4 số hoặc localhost, chỉ khớp khi giống hệt nhau
        if (isNumericIP(ip1) || isNumericIP(ip2) || ip1.equals("localhost") || ip2.equals("localhost")) {
            return ip1.equalsIgnoreCase(ip2);
        }

        // Kiểm tra Subdomain hợp lệ (ví dụ play.aemine.vn là subdomain của aemine.vn)
        if (isSubdomainOf(ip1, ip2) || isSubdomainOf(ip2, ip1)) {
            return true;
        }

        // So khớp Root Domain (ví dụ sky.aemine.vn và lobby.aemine.vn cùng thuộc aemine.vn)
        String root1 = extractRootDomain(ip1);
        String root2 = extractRootDomain(ip2);
        if (!root1.isEmpty() && root1.equalsIgnoreCase(root2)) {
            return true;
        }

        return false;
    }

    private static boolean isNumericIP(String host) {
        if (host == null || host.isEmpty()) return false;
        String[] parts = host.split("\\.");
        if (parts.length != 4) return false;
        for (String p : parts) {
            try {
                int n = Integer.parseInt(p);
                if (n < 0 || n > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSubdomainOf(String sub, String parent) {
        if (sub == null || parent == null) return false;
        if (!parent.contains(".")) return false;
        return sub.endsWith("." + parent);
    }

    private static String extractRootDomain(String host) {
        if (host == null || host.isEmpty()) return "";
        String[] parts = host.split("\\.");
        if (parts.length < 2) return host;

        int len = parts.length;
        if (len >= 3) {
            String secondLast = parts[len - 2].toLowerCase();
            String last = parts[len - 1].toLowerCase();
            if ((secondLast.equals("com") || secondLast.equals("net") || secondLast.equals("org") || secondLast.equals("edu") || secondLast.equals("gov") || secondLast.equals("co"))
                    && (last.equals("vn") || last.equals("uk") || last.equals("au") || last.equals("br") || last.equals("jp"))) {
                return parts[len - 3] + "." + secondLast + "." + last;
            }
        }

        return parts[len - 2] + "." + parts[len - 1];
    }

    /**
     * Ghi nhận lệnh xác thực đang chờ Server kiểm tra và đồng ý
     */
    public synchronized void recordPendingAuth(String username, String serverIP, String password, boolean autoLogin, String commandType) {
        if (username == null || serverIP == null || password == null) return;
        this.pendingAuth = new PendingAuth(username, cleanServerIP(serverIP), password, autoLogin, commandType);
        CasxDuoisClient.LOGGER.info("[ServerAuth] Đã ghi nhận lệnh " + commandType + " (AutoLogin: " + autoLogin + ") cho " + username + " tại " + serverIP + ", đang chờ Server xác nhận...");
    }

    public synchronized boolean isRecentAutoLogin(String username, String serverIP, String password) {
        PendingAuth pa = this.pendingAuth;
        if (pa == null || pa.isExpired(3000)) return false;
        return pa.isAutoLogin()
                && pa.getUsername().equalsIgnoreCase(username)
                && pa.getServerIP().equalsIgnoreCase(cleanServerIP(serverIP))
                && pa.getPassword().equals(password);
    }

    public synchronized void clearPendingAuth() {
        this.pendingAuth = null;
    }

    public synchronized PendingAuth getPendingAuth() {
        return this.pendingAuth;
    }

    /**
     * Xử lý gói tin Chat hoặc Title gửi từ máy chủ về Client để kiểm tra phản hồi đăng nhập
     */
    public synchronized void handleServerMessage(String rawMessage, boolean isTitle) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) return;

        // Bỏ qua tin nhắn chat của người chơi khác theo định dạng chuẩn <PlayerName>
        if (!isTitle && rawMessage.matches("(?s).*<[a-zA-Z0-9_]{3,16}>.*")) {
            return;
        }

        String normalized = normalizeText(rawMessage);
        if (normalized.isEmpty()) return;

        // 1. Kiểm tra nếu tin nhắn là báo SAI mật khẩu / thất bại
        if (matchesFailure(normalized)) {
            handleAuthFailure();
            return;
        }

        // 2. Kiểm tra nếu tin nhắn là báo ĐÚNG mật khẩu / thành công
        if (matchesSuccess(normalized)) {
            handleAuthSuccess();
        }
    }

    /**
     * Khi Server thông báo sai mật khẩu: Hủy bỏ lưu mật khẩu vừa nhập hoặc xóa mật khẩu cũ bị sai
     */
    private synchronized void handleAuthFailure() {
        PendingAuth pa = this.pendingAuth;
        if (pa == null || pa.isExpired(15000)) {
            this.pendingAuth = null;
            return;
        }

        String username = pa.getUsername();
        String serverIP = pa.getServerIP();
        boolean autoLogin = pa.isAutoLogin();

        CasxDuoisClient.LOGGER.warn("[ServerAuth] Server " + serverIP + " từ chối mật khẩu của nick " + username + " (AutoLogin: " + autoLogin + ")");

        if (autoLogin) {
            // Mật khẩu đã lưu trên máy chủ bị sai -> Hủy mật khẩu đã lưu và cảnh báo để bảo vệ nick không bị ban
            setPassword(username, serverIP, null);
            sendClientMessage("§8[§bCasxDuois§8] §c✖ [Auto Login] Server báo sai mật khẩu đã lưu! Đã hủy tự động đăng nhập server này để bảo vệ tài khoản của bạn.");
            syncToModule();
        } else {
            // Người chơi gõ mật khẩu sai -> TUYỆT ĐỐI KHÔNG LƯU
            sendClientMessage("§8[§bCasxDuois§8] §c✖ Server thông báo sai mật khẩu! Hệ thống KHÔNG lưu mật khẩu này.");
        }

        this.pendingAuth = null;
    }

    /**
     * Khi Server đồng ý và xác nhận đúng mật khẩu: Tiến hành lưu mật khẩu vào server_auth.json
     */
    private synchronized void handleAuthSuccess() {
        PendingAuth pa = this.pendingAuth;
        if (pa != null && !pa.isExpired(15000)) {
            String username = pa.getUsername();
            String serverIP = pa.getServerIP();
            String password = pa.getPassword();
            boolean autoLogin = pa.isAutoLogin();
            String cmdType = pa.getCommandType();

            if (autoLogin) {
                CasxDuoisClient.LOGGER.info("✔ [AutoLogin] Server " + serverIP + " đã xác nhận đăng nhập thành công cho nick " + username);
                sendClientMessage("§8[§bCasxDuois§8] §a✔ Tự động đăng nhập thành công vào máy chủ §e" + serverIP + "§a!");
            } else {
                // Người chơi nhập đúng mật khẩu và Server ĐÃ ĐỒNG Ý -> LƯU MẬT KHẨU VÀO FILE
                setPassword(username, serverIP, password);
                CasxDuoisClient.LOGGER.info("✔ [ServerAuth] Server đã xác nhận đúng mật khẩu! Đã lưu tự động đăng nhập cho " + username + " tại " + serverIP);

                if ("REGISTER".equalsIgnoreCase(cmdType)) {
                    sendClientMessage("§8[§bCasxDuois§8] §a✔ Server đã xác nhận đăng ký thành công! Đã lưu mật khẩu tự động cho nick §e" + username + " §atại server §e" + serverIP + "§a.");
                } else if ("CHANGE_PASSWORD".equalsIgnoreCase(cmdType)) {
                    sendClientMessage("§8[§bCasxDuois§8] §a✔ Server đã xác nhận đổi mật khẩu thành công! Đã cập nhật mật khẩu mới cho nick §e" + username + " §atại server §e" + serverIP + "§a.");
                } else {
                    sendClientMessage("§8[§bCasxDuois§8] §a✔ Server đã xác nhận đúng mật khẩu! Đã lưu tự động đăng nhập cho nick §e" + username + " §atại server §e" + serverIP + "§a.");
                }

                syncToModule();
            }

            this.pendingAuth = null;
        }

        // Kích hoạt lệnh chuyển sảnh (Post Command) nếu có sau khi đăng nhập thành công
        triggerPostCommand();
    }

    private void syncToModule() {
        if (CasxDuoisClient.getInstance() != null && CasxDuoisClient.getInstance().getModuleManager() != null) {
            AutoReconnectModule autoReconnect = CasxDuoisClient.getInstance().getModuleManager().getModule(AutoReconnectModule.class);
            if (autoReconnect != null) {
                autoReconnect.syncCurrentServerPassword();
            }
        }
    }

    private void triggerPostCommand() {
        if (CasxDuoisClient.getInstance() != null && CasxDuoisClient.getInstance().getModuleManager() != null) {
            AutoReconnectModule autoReconnect = CasxDuoisClient.getInstance().getModuleManager().getModule(AutoReconnectModule.class);
            if (autoReconnect != null) {
                autoReconnect.onLoginSuccess();
            }
        }
    }

    public void sendClientMessage(String msg) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(() -> {
                if (mc.player != null) {
                    mc.player.sendMessage(new TextComponentString(msg));
                }
            });
        }
    }

    /**
     * Chuẩn hóa văn bản: loại bỏ mã màu (§, &), bỏ dấu tiếng Việt (NFD), xóa ký tự đặc biệt và chuyển chữ thường
     */
    public static String normalizeText(String input) {
        if (input == null) return "";
        String text = input.replaceAll("(?i)[§&][0-9a-fk-or]", " ");
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String noAccents = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(decomposed).replaceAll("");
        noAccents = noAccents.replace('đ', 'd').replace('Đ', 'D');
        return noAccents.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    public static boolean matchesFailure(String normalized) {
        for (String pattern : FAILURE_PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesSuccess(String normalized) {
        for (String pattern : SUCCESS_PATTERNS) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}


