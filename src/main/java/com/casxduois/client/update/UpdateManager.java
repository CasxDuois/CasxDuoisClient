package com.casxduois.client.update;

import com.casxduois.client.CasxDuoisClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * UpdateManager: Quản lý kiểm tra và thông báo bản cập nhật mới từ GitHub Releases API
 * Repo: https://github.com/CasxDuois/CasxDuoisClient
 */
public class UpdateManager {

    private static final UpdateManager INSTANCE = new UpdateManager();

    public static final String REPO_OWNER = "CasxDuois";
    public static final String REPO_NAME = "CasxDuoisClient";
    public static final String API_URL = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
    public static final String REPO_RELEASES_URL = "https://github.com/" + REPO_OWNER + "/" + REPO_NAME + "/releases";

    private boolean checking = false;
    private boolean checked = false;
    private boolean hasUpdate = false;
    private boolean modalDismissed = false;
    private boolean chatNotified = false;

    private String latestVersion = "";
    private String releaseTitle = "";
    private String changelog = "";
    private String releaseUrl = REPO_RELEASES_URL;
    private String downloadUrl = REPO_RELEASES_URL;

    private UpdateManager() {}

    public static UpdateManager getInstance() {
        return INSTANCE;
    }

    /**
     * Bắt đầu kiểm tra bản cập nhật mới ở luồng nền (Asynchronous Daemon Thread)
     */
    public void checkUpdatesAsync() {
        if (checking) return;
        checking = true;

        Thread thread = new Thread(() -> {
            try {
                CasxDuoisClient.LOGGER.info("Đang kiểm tra bản cập nhật mới từ GitHub (" + REPO_OWNER + "/" + REPO_NAME + ")...");
                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "CasxDuoisClient/" + CasxDuoisClient.CLIENT_VERSION);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }

                    JsonObject root = new JsonParser().parse(sb.toString()).getAsJsonObject();

                    String tag = root.has("tag_name") && !root.get("tag_name").isJsonNull()
                            ? root.get("tag_name").getAsString()
                            : "";
                    this.latestVersion = tag.trim().replaceAll("^[vV]", "");

                    this.releaseTitle = root.has("name") && !root.get("name").isJsonNull()
                            ? root.get("name").getAsString()
                            : ("CasxDuois v" + this.latestVersion);

                    this.changelog = root.has("body") && !root.get("body").isJsonNull()
                            ? root.get("body").getAsString()
                            : "";

                    this.releaseUrl = root.has("html_url") && !root.get("html_url").isJsonNull()
                            ? root.get("html_url").getAsString()
                            : REPO_RELEASES_URL;

                    this.downloadUrl = this.releaseUrl;

                    // Tìm link tải trực tiếp file .jar nếu có đính kèm trong assets
                    if (root.has("assets") && root.get("assets").isJsonArray()) {
                        JsonArray assets = root.getAsJsonArray("assets");
                        for (JsonElement el : assets) {
                            if (el.isJsonObject()) {
                                JsonObject asset = el.getAsJsonObject();
                                String name = asset.has("name") ? asset.get("name").getAsString() : "";
                                if (name.toLowerCase().endsWith(".jar") && asset.has("browser_download_url")) {
                                    this.downloadUrl = asset.get("browser_download_url").getAsString();
                                    break;
                                }
                            }
                        }
                    }

                    this.hasUpdate = isNewerVersion(this.latestVersion, CasxDuoisClient.CLIENT_VERSION);

                    if (this.hasUpdate) {
                        CasxDuoisClient.LOGGER.info("Phát hiện bản cập nhật mới: v" + this.latestVersion + " (Hiện tại: v" + CasxDuoisClient.CLIENT_VERSION + ")");
                    } else {
                        CasxDuoisClient.LOGGER.info("Bạn đang sử dụng phiên bản mới nhất (v" + CasxDuoisClient.CLIENT_VERSION + ").");
                    }
                } else if (code == 404) {
                    CasxDuoisClient.LOGGER.info("Chưa có bản phát hành (Release) nào trên GitHub " + REPO_OWNER + "/" + REPO_NAME);
                } else {
                    CasxDuoisClient.LOGGER.warn("Kiểm tra cập nhật GitHub thất bại, mã HTTP: " + code);
                }
            } catch (Throwable t) {
                CasxDuoisClient.LOGGER.debug("Không thể kết nối đến GitHub Releases API để kiểm tra cập nhật: " + t.getMessage());
            } finally {
                this.checking = false;
                this.checked = true;
            }
        }, "CasxDuois-UpdateChecker");

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * So sánh phiên bản SemVer (Semantic Versioning)
     * Ví dụ: 1.1.0 > 1.0.0 (true), 1.0.1 > 1.0.0 (true)
     */
    public static boolean isNewerVersion(String remote, String current) {
        if (remote == null || current == null || remote.trim().isEmpty() || current.trim().isEmpty()) {
            return false;
        }

        String rClean = remote.trim().replaceAll("^[vV]", "");
        String cClean = current.trim().replaceAll("^[vV]", "");

        String[] rParts = rClean.split("[.-]");
        String[] cParts = cClean.split("[.-]");

        int maxLen = Math.max(rParts.length, cParts.length);
        for (int i = 0; i < maxLen; i++) {
            int rNum = 0;
            int cNum = 0;
            if (i < rParts.length) {
                try {
                    rNum = Integer.parseInt(rParts[i]);
                } catch (NumberFormatException ignored) {}
            }
            if (i < cParts.length) {
                try {
                    cNum = Integer.parseInt(cParts[i]);
                } catch (NumberFormatException ignored) {}
            }
            if (rNum > cNum) return true;
            if (rNum < cNum) return false;
        }
        return false;
    }

    /**
     * Mở link tải bản mới hoặc trang GitHub Release trên trình duyệt mặc định
     */
    public void openDownloadLink() {
        String targetUrl = (downloadUrl != null && !downloadUrl.isEmpty()) ? downloadUrl : releaseUrl;
        openWebUrl(targetUrl);
    }

    /**
     * Mở link web an toàn, hỗ trợ Windows, Mac, Linux
     */
    public static void openWebUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Throwable ignored) {}

        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Throwable t) {
            CasxDuoisClient.LOGGER.error("Không thể mở trình duyệt cho URL: " + url, t);
        }
    }

    /**
     * Gửi tin nhắn thông báo cập nhật vào khung chat khi người chơi vào game/server
     */
    public void notifyInChatIfEligible() {
        if (!hasUpdate || chatNotified) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        chatNotified = true;

        TextComponentString prefix = new TextComponentString("§8[§f✦ CasxDuois§8] §fĐã có bản cập nhật mới §a§lv" + latestVersion + "§f! ");
        TextComponentString btn = new TextComponentString("§e§n[Bấm vào đây để tải]");
        Style btnStyle = new Style();
        btnStyle.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));
        btnStyle.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("§7Nhấp để mở trang tải phiên bản mới")));
        btn.setStyle(btnStyle);

        prefix.appendSibling(btn);
        mc.player.sendMessage(prefix);
    }

    public boolean hasUpdate() {
        return hasUpdate;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isChecking() {
        return checking;
    }

    public boolean isModalDismissed() {
        return modalDismissed;
    }

    public void setModalDismissed(boolean modalDismissed) {
        this.modalDismissed = modalDismissed;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getReleaseTitle() {
        return releaseTitle;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getReleaseUrl() {
        return releaseUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
