package com.casxduois.client.modules.utility;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.config.StringSetting;
import com.casxduois.client.discord.DiscordRESTPoller;
import com.casxduois.client.discord.DiscordWebhookSender;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import org.lwjgl.input.Keyboard;

/**
 * DiscordRemoteModule: Giám sát tài khoản từ xa qua lệnh !tra trên Discord
 * Hỗ trợ cấu hình Webhook URL, Bot Token, Channel ID và nút Test kết nối.
 */
public class DiscordRemoteModule extends Module {

    public final StringSetting webhookUrl = new StringSetting("Webhook URL", "Webhook URL", "Địa chỉ webhook Discord để nhận thông báo và ảnh", "");
    public final StringSetting botToken = new StringSetting("Bot Token", "Bot Token", "Mã token bot Discord để nhận lệnh từ bạn", "", true);
    public final StringSetting channelId = new StringSetting("Channel ID", "ID Kênh Chat", "ID phòng chat Discord nơi bạn gửi lệnh", "");
    public final NumberSetting pollInterval = new NumberSetting("Poll Interval", "Thời gian quét (giây)", "Khoảng cách thời gian kiểm tra tin nhắn mới trên Discord", 5.0, 3.0, 15.0, 1.0);

    public DiscordRemoteModule() {
        super("DiscordRemote", "Điều Khiển Discord", "Gửi thông báo trạng thái và nhận lệnh từ xa qua Discord", Category.WEBHOOK, Keyboard.KEY_NONE);
        addSetting(webhookUrl);
        addSetting(botToken);
        addSetting(channelId);
        addSetting(pollInterval);
    }

    private int tickCounter = 0;

    @Override
    public void onEnable() {
        restartPoller();
    }

    @Override
    public void onDisable() {
        if (CasxDuoisClient.getInstance().getDiscordPoller() != null) {
            CasxDuoisClient.getInstance().getDiscordPoller().stop();
        }
    }

    @Override
    public void onTick() {
        // Cơ chế tự phục hồi (Watchdog): Mỗi 2 giây kiểm tra xem Worker đã chạy chưa
        if (++tickCounter % 40 == 0) {
            DiscordRESTPoller poller = CasxDuoisClient.getInstance().getDiscordPoller();
            if (poller != null && !poller.isRunning() && isToggled()) {
                if (!botToken.getValue().trim().isEmpty() && !channelId.getValue().trim().isEmpty()) {
                    syncPollerSettings();
                    poller.start();
                }
            }
        }
    }

    public void syncPollerSettings() {
        DiscordRESTPoller poller = CasxDuoisClient.getInstance().getDiscordPoller();
        if (poller != null) {
            poller.setWebhookUrl(webhookUrl.getValue());
            poller.setBotToken(botToken.getValue());
            poller.setChannelId(channelId.getValue());
            poller.setPollIntervalSeconds(pollInterval.getValueInt());
            poller.saveConfig();
        }
    }

    public synchronized void restartPoller() {
        syncPollerSettings();
        DiscordRESTPoller poller = CasxDuoisClient.getInstance().getDiscordPoller();
        if (poller != null) {
            poller.stop();
            if (isToggled() && !botToken.getValue().trim().isEmpty() && !channelId.getValue().trim().isEmpty()) {
                poller.start();
            }
        }
    }

    /**
     * Gửi tin nhắn mẫu kiểm tra kết nối Webhook và đồng thời kích hoạt Poller
     */
    public void testConnection() {
        restartPoller();

        String url = webhookUrl.getValue();
        if (url == null || url.trim().isEmpty()) {
            CasxDuoisClient.LOGGER.warn("Webhook URL đang để trống, không thể test!");
            return;
        }

        String myUsername = (mc.player != null) ? mc.player.getName() : (mc.getSession() != null ? mc.getSession().getUsername() : "Chưa Đăng Nhập");
        String chId = channelId.getValue().trim();

        String json = "{\n" +
                "  \"username\": \"CasxDuois Client Test\",\n" +
                "  \"embeds\": [{\n" +
                "    \"title\": \"Kết Nối Thành Công\",\n" +
                "    \"description\": \"Webhook và Bot đã kết nối chuẩn xác với CasxDuois Client.\\n• Tài khoản hiện tại: **" + myUsername + "**\\n• Channel đọc lệnh: <#" + chId + ">\\n• Trạng thái Worker: **ĐANG LẮNG NGHE LỆNH**\\n\\nSẵn sàng nhận lệnh **!tra**, **!tra <tên>** hoặc **!tra all**!\",\n" +
                "    \"color\": 62206,\n" +
                "    \"footer\": { \"text\": \"CasxDuois Client v1.0 • Ready\" }\n" +
                "  }]\n" +
                "}";

        DiscordWebhookSender.sendAsync(url, json, null, null);
        CasxDuoisClient.LOGGER.info("✔ Đã gửi tin nhắn test tới Discord Webhook và kích hoạt Worker!");
    }
}
