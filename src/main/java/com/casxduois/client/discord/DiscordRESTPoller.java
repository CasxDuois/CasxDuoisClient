package com.casxduois.client.discord;

import com.casxduois.client.CasxDuoisClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * DiscordRESTPoller: Luồng ngầm định kỳ poll Discord REST API kiểm tra lệnh !tra <tên>, !tra all, !tra
 * Hỗ trợ nhận diện tài khoản linh hoạt, theo dõi snowflake ID chuẩn xác và ghi log chi tiết.
 */
public class DiscordRESTPoller {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private String botToken = "";
    private String channelId = "";
    private String webhookUrl = "";
    private int pollIntervalSeconds = 5;

    private Thread pollerThread;
    private volatile boolean running = false;
    private String lastMessageId = null;
    private boolean initializedHistory = false;

    private final File configFile;

    public DiscordRESTPoller(File clientDir) {
        this.configFile = new File(clientDir, "discord.json");
        loadConfig();
    }

    public synchronized void start() {
        if (running) return;

        if (botToken == null || botToken.trim().isEmpty() || channelId == null || channelId.trim().isEmpty()) {
            CasxDuoisClient.LOGGER.warn("[DiscordRemote] Chưa thể khởi động: Bot Token hoặc Channel ID đang để trống!");
            return;
        }

        running = true;
        pollerThread = new Thread(this::pollLoop, "DiscordRESTPoller-Worker");
        pollerThread.setDaemon(true);
        pollerThread.start();
        CasxDuoisClient.LOGGER.info("[DiscordRemote] Đã khởi động luồng Worker lắng nghe Discord REST API!");
    }

    public synchronized void stop() {
        running = false;
        if (pollerThread != null) {
            pollerThread.interrupt();
            pollerThread = null;
        }
        initializedHistory = false;
        CasxDuoisClient.LOGGER.info("[DiscordRemote] Đã dừng luồng Worker lắng nghe Discord.");
    }

    private void pollLoop() {
        // Nghỉ 500ms khởi đầu
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            return;
        }

        while (running) {
            try {
                checkDiscordMessages();
            } catch (Exception e) {
                CasxDuoisClient.LOGGER.error("[DiscordRemote] Lỗi ngoại lệ vòng lặp poll: " + e.getMessage());
            }

            try {
                Thread.sleep(Math.max(2000, pollIntervalSeconds * 1000L));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void checkDiscordMessages() {
        if (botToken == null || botToken.trim().isEmpty() || channelId == null || channelId.trim().isEmpty()) return;

        String endpoint = "https://discord.com/api/v10/channels/" + channelId.trim() + "/messages?limit=10";
        if (lastMessageId != null && initializedHistory) {
            endpoint += "&after=" + lastMessageId;
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bot " + botToken.trim());
            conn.setRequestProperty("User-Agent", "CasxDuoisClient (Minecraft 1.12.2, 1.0)");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            if (status == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement parsed = new JsonParser().parse(reader);
                    if (parsed.isJsonArray()) {
                        JsonArray messages = parsed.getAsJsonArray();

                        // Tìm snowflake ID lớn nhất trong đợt tải về
                        long maxIdInBatch = 0;
                        for (int i = 0; i < messages.size(); i++) {
                            try {
                                long id = Long.parseUnsignedLong(messages.get(i).getAsJsonObject().get("id").getAsString());
                                if (id > maxIdInBatch) maxIdInBatch = id;
                            } catch (Exception ignored) {}
                        }

                        if (!initializedHistory) {
                            if (maxIdInBatch > 0) {
                                lastMessageId = Long.toUnsignedString(maxIdInBatch);
                            }
                            initializedHistory = true;
                            CasxDuoisClient.LOGGER.info("✔ [DiscordRemote] Đã kết nối Bot và theo dõi Channel " + channelId + " (lastMessageId = " + lastMessageId + ")");
                            return;
                        }

                        if (maxIdInBatch > 0) {
                            lastMessageId = Long.toUnsignedString(maxIdInBatch);
                        }

                        // Xử lý từ tin cũ hơn đến mới hơn (messages[0] là mới nhất)
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            JsonObject msg = messages.get(i).getAsJsonObject();

                            // Bỏ qua tin nhắn do Bot hoặc Webhook gửi
                            if (msg.has("author")) {
                                JsonObject author = msg.getAsJsonObject("author");
                                if (author.has("bot") && author.get("bot").getAsBoolean()) {
                                    continue;
                                }
                            }

                            if (msg.has("content")) {
                                String content = msg.get("content").getAsString().trim();
                                if (!content.isEmpty()) {
                                    processCommand(content);
                                }
                            }
                        }
                    }
                }
            } else if (status == 429) {
                CasxDuoisClient.LOGGER.warn("[DiscordRemote] Bị Rate Limit (429)! Tạm dừng 5 giây.");
                Thread.sleep(5000);
            } else if (status == 401) {
                CasxDuoisClient.LOGGER.error("[DiscordRemote] Bot Token không hợp lệ (HTTP 401)! Hãy kiểm tra lại Bot Token.");
            } else if (status == 403) {
                CasxDuoisClient.LOGGER.error("[DiscordRemote] Bot không có quyền đọc tin nhắn trong Channel " + channelId + " (HTTP 403)! Hãy phân quyền 'View Channel' và 'Read Message History' cho Bot.");
            } else if (status == 404) {
                CasxDuoisClient.LOGGER.error("[DiscordRemote] Không tìm thấy Channel ID " + channelId + " (HTTP 404)!");
            } else {
                CasxDuoisClient.LOGGER.warn("[DiscordRemote] API trả về HTTP " + status);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && !e.getMessage().contains("timed out")) {
                CasxDuoisClient.LOGGER.warn("[DiscordRemote] Lỗi kết nối API: " + e.getMessage());
            }
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    private String getEffectiveUsername() {
        if (mc.player != null && mc.player.getName() != null && !mc.player.getName().isEmpty()) {
            return mc.player.getName().trim().toLowerCase();
        }
        if (mc.getSession() != null && mc.getSession().getUsername() != null) {
            return mc.getSession().getUsername().trim().toLowerCase();
        }
        return "";
    }

    private void processCommand(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String raw = text.trim();
        String lower = raw.toLowerCase();

        // 1. Kiểm tra tiền tố lệnh
        if (!lower.startsWith("!tra") && !lower.startsWith("/tra") && !lower.startsWith("!ping")) {
            return;
        }

        String myUsername = getEffectiveUsername();
        CasxDuoisClient.LOGGER.info("[DiscordRemote] Nhận lệnh từ Discord: '" + raw + "' | Nick hiện tại: '" + myUsername + "'");

        // Lệnh Ping
        if (lower.equals("!ping") || lower.equals("/ping")) {
            ScreenshotService.sendQuickMessage(webhookUrl, "🏓 **PONG!** Client của **" + (myUsername.isEmpty() ? "Tab Game" : myUsername) + "** đang hoạt động bình thường.");
            return;
        }

        // Tách lệnh !tra
        String[] parts = raw.split("\\s+", 2);
        if (parts.length == 1) {
            // Người dùng chỉ gõ đúng "!tra": Chụp và gửi ảnh tài khoản hiện tại ngay lập tức!
            CasxDuoisClient.LOGGER.info("[DiscordRemote] Đang thực thi lệnh '!tra' (chụp F2) cho: " + myUsername);
            ScreenshotService.captureAndSend(webhookUrl);
            return;
        }

        String arg = parts[1].trim().toLowerCase();

        if (arg.equals("all")) {
            // Lệnh "!tra all": Ép chụp F2 gửi về
            CasxDuoisClient.LOGGER.info("[DiscordRemote] Đang thực thi lệnh '!tra all' cho: " + myUsername);
            ScreenshotService.captureAndSend(webhookUrl);
        } else if (arg.equals("status")) {
            // Lệnh "!tra status": Gửi bảng thông số embed
            CasxDuoisClient.LOGGER.info("[DiscordRemote] Đang thực thi lệnh '!tra status' cho: " + myUsername);
            ScreenshotService.sendStatusOnly(webhookUrl);
        } else {
            // Lệnh "!tra <username>"
            if (arg.equals(myUsername) || myUsername.equalsIgnoreCase(arg) || (!myUsername.isEmpty() && (myUsername.contains(arg) || arg.contains(myUsername)))) {
                CasxDuoisClient.LOGGER.info("[DiscordRemote] Khớp tên tài khoản ('" + arg + "' == '" + myUsername + "')! Đang chụp ảnh...");
                ScreenshotService.captureAndSend(webhookUrl);
            } else {
                CasxDuoisClient.LOGGER.info("[DiscordRemote] Lệnh nhắm tới tài khoản '" + arg + "', nick hiện tại là '" + myUsername + "', bỏ qua.");
            }
        }
    }

    public synchronized void loadConfig() {
        if (!configFile.exists()) return;

        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root.has("botToken")) botToken = root.get("botToken").getAsString();
            if (root.has("channelId")) channelId = root.get("channelId").getAsString();
            if (root.has("webhookUrl")) webhookUrl = root.get("webhookUrl").getAsString();
            if (root.has("pollInterval")) pollIntervalSeconds = root.get("pollInterval").getAsInt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveConfig() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            JsonObject root = new JsonObject();
            root.addProperty("botToken", botToken);
            root.addProperty("channelId", channelId);
            root.addProperty("webhookUrl", webhookUrl);
            root.addProperty("pollInterval", pollIntervalSeconds);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public boolean isRunning() {
        return running;
    }
}
