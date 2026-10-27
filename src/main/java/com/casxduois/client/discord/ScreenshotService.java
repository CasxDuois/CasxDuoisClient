package com.casxduois.client.discord;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.core.MultiTabManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ScreenShotHelper;
import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * ScreenshotService: Chụp ảnh game chuẩn F2 (kể cả khi tab đang chạy ngầm) và đóng gói Embed gửi về Discord
 */
public class ScreenshotService {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final SimpleDateFormat ISO_FORMAT;

    static {
        ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void sendQuickMessage(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) return;
        String json = "{\"content\": \"" + message.replace("\"", "\\\"") + "\"}";
        DiscordWebhookSender.sendAsync(webhookUrl, json, null, null);
    }

    /**
     * Chụp ảnh màn hình hiện tại kèm Embed thông số và gửi lên Webhook
     */
    public static void captureAndSend(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            CasxDuoisClient.LOGGER.warn("[DiscordRemote] Webhook URL chưa được cài đặt!");
            return;
        }

        // BẮT BUỘC thực thi việc chụp trên Main Render Thread
        mc.addScheduledTask(() -> {
            try {
                // Nếu cửa sổ đang chạy ngầm / skip render, ép vẽ 1 frame hoàn chỉnh
                if (!Display.isActive() || MultiTabManager.skipWorldRender) {
                    MultiTabManager.forceRenderOneFrame();
                }

                int w = mc.displayWidth > 0 ? mc.displayWidth : 854;
                int h = mc.displayHeight > 0 ? mc.displayHeight : 480;

                BufferedImage screenshot = null;
                try {
                    screenshot = ScreenShotHelper.createScreenshot(w, h, mc.getFramebuffer());
                } catch (Throwable t) {
                    CasxDuoisClient.LOGGER.error("[ScreenshotService] Không thể chụp Framebuffer: " + t.getMessage());
                }

                // Thu thập thông số telemetry
                TelemetryData data = collectTelemetry();

                if (screenshot == null) {
                    CasxDuoisClient.LOGGER.warn("[ScreenshotService] Ảnh chụp null, tự động chuyển sang gửi Bảng trạng thái Embed.");
                    sendStatusOnly(webhookUrl);
                    return;
                }

                final BufferedImage img = screenshot;
                // Chuyển việc encode PNG và gửi HTTP ra luồng ngầm để không khựng game
                new Thread(() -> {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(img, "png", baos);
                        byte[] pngBytes = baos.toByteArray();

                        String jsonPayload = buildEmbedJson(data, true);
                        DiscordWebhookSender.send(webhookUrl, jsonPayload, pngBytes, "screenshot.png");
                        CasxDuoisClient.LOGGER.info("✔ [DiscordRemote] Đã gửi ảnh F2 & Embed thành công lên Discord cho: " + data.username);
                    } catch (Exception e) {
                        CasxDuoisClient.LOGGER.error("[DiscordRemote] Lỗi khi gửi ảnh lên Webhook: " + e.getMessage());
                    }
                }, "Screenshot-Encoder").start();

            } catch (Throwable t) {
                CasxDuoisClient.LOGGER.error("[ScreenshotService] Ngoại lệ trên Main Thread: " + t.getMessage());
            }
        });
    }

    /**
     * Gửi bảng trạng thái dạng text/embed (dùng cho lệnh !tra status hoặc fallback)
     */
    public static void sendStatusOnly(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) return;

        mc.addScheduledTask(() -> {
            TelemetryData data = collectTelemetry();
            new Thread(() -> {
                try {
                    String jsonPayload = buildEmbedJson(data, false);
                    DiscordWebhookSender.send(webhookUrl, jsonPayload, null, null);
                    CasxDuoisClient.LOGGER.info("✔ [DiscordRemote] Đã gửi Bảng trạng thái thành công cho: " + data.username);
                } catch (Exception e) {
                    CasxDuoisClient.LOGGER.error("[DiscordRemote] Lỗi gửi Bảng trạng thái: " + e.getMessage());
                }
            }, "Status-Sender").start();
        });
    }

    private static TelemetryData collectTelemetry() {
        TelemetryData d = new TelemetryData();
        d.username = mc.getSession() != null ? mc.getSession().getUsername() : "Chưa Đăng Nhập";

        if (mc.getCurrentServerData() != null) {
            d.serverIP = mc.getCurrentServerData().serverIP;
        } else if (mc.isIntegratedServerRunning()) {
            d.serverIP = "Chơi Đơn (Singleplayer)";
        } else {
            d.serverIP = "Sảnh Chờ / Chưa Vào Server";
        }

        if (mc.player != null) {
            d.hp = String.format("%.1f / %.1f ❤", mc.player.getHealth(), mc.player.getMaxHealth());
            d.food = mc.player.getFoodStats().getFoodLevel() + " / 20 🍖";
            d.level = String.valueOf(mc.player.experienceLevel);
            d.xyz = String.format("X: %.1f | Y: %.1f | Z: %.1f", mc.player.posX, mc.player.posY, mc.player.posZ);
        } else {
            d.hp = "N/A";
            d.food = "N/A";
            d.level = "N/A";
            d.xyz = "N/A";
        }

        // Ping
        if (mc.getConnection() != null && mc.player != null) {
            NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
            d.ping = (info != null) ? info.getResponseTime() + " ms" : "N/A";
        } else {
            d.ping = "N/A";
        }

        // FPS & RAM
        d.fps = Minecraft.getDebugFPS() + " FPS";
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        d.ram = usedMem + "MB / " + totalMem + "MB";

        return d;
    }

    private static String buildEmbedJson(TelemetryData data, boolean includeImage) {
        JsonObject root = new JsonObject();
        root.addProperty("username", "CasxDuois Inspector");
        root.addProperty("avatar_url", "https://i.imgur.com/8Qe5kI7.png");

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        embed.addProperty("title", "CasxDuois Client | Thông Tin");
        embed.addProperty("description", "Báo Cáo: **" + data.username + "**");
        embed.addProperty("color", 62206); // Neon Cyan #00F2FE

        JsonArray fields = new JsonArray();
        fields.add(makeField("Tài Khoản", data.username, true));
        fields.add(makeField("Máy Chủ", data.serverIP, true));

        embed.add("fields", fields);

        if (includeImage) {
            JsonObject imageObj = new JsonObject();
            imageObj.addProperty("url", "attachment://screenshot.png");
            embed.add("image", imageObj);
        }

        embed.addProperty("timestamp", ISO_FORMAT.format(new Date()));

        embeds.add(embed);
        root.add("embeds", embeds);

        return root.toString();
    }

    private static JsonObject makeField(String name, String value, boolean inline) {
        JsonObject f = new JsonObject();
        f.addProperty("name", name);
        f.addProperty("value", value);
        f.addProperty("inline", inline);
        return f;
    }

    private static class TelemetryData {
        String username;
        String serverIP;
        String ping;
        String hp;
        String food;
        String level;
        String xyz;
        String fps;
        String ram;
    }
}
