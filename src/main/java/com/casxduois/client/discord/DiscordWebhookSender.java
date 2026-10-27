package com.casxduois.client.discord;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DiscordWebhookSender: Gửi tin nhắn Embed và File đính kèm (ảnh chụp màn hình) lên Discord Webhook
 * Sử dụng thuần túy HttpURLConnection trên ExecutorService riêng, không làm giật khung hình Minecraft.
 */
public class DiscordWebhookSender {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DiscordWebhook-Worker");
        t.setDaemon(true);
        return t;
    });

    public static void sendAsync(String webhookUrl, String jsonPayload, byte[] fileData, String fileName) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) return;

        EXECUTOR.submit(() -> {
            try {
                send(webhookUrl, jsonPayload, fileData, fileName);
            } catch (Exception e) {
                System.err.println("[DiscordWebhook] Lỗi gửi tin nhắn: " + e.getMessage());
            }
        });
    }

    public static void send(String webhookUrl, String jsonPayload, byte[] fileData, String fileName) throws Exception {
        URL url = new URL(webhookUrl.trim());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("User-Agent", "CasxDuoisClient/1.0 (Minecraft 1.12.2)");

        if (fileData != null && fileData.length > 0) {
            // Gửi Multipart Form-Data (kèm hình ảnh)
            String boundary = "----CasxDuoisClientBoundary" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 1. Phần JSON Embed
            if (jsonPayload != null && !jsonPayload.isEmpty()) {
                baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                baos.write("Content-Disposition: form-data; name=\"payload_json\"\r\n".getBytes(StandardCharsets.UTF_8));
                baos.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                baos.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }

            // 2. Phần File ảnh PNG
            String uploadName = (fileName != null && !fileName.isEmpty()) ? fileName : "screenshot.png";
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + uploadName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write(fileData);
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));

            // Kết thúc boundary
            baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            byte[] fullBytes = baos.toByteArray();
            conn.setRequestProperty("Content-Length", String.valueOf(fullBytes.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(fullBytes);
                os.flush();
            }
        } else {
            // Gửi JSON thuần
            byte[] jsonBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(jsonBytes.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBytes);
                os.flush();
            }
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            // Thành công
            try (InputStream is = conn.getInputStream()) {
                byte[] buf = new byte[512];
                while (is.read(buf) != -1) {}
            }
        } else {
            System.err.println("[DiscordWebhook] Phản hồi lỗi HTTP: " + code);
        }
        conn.disconnect();
    }
}
