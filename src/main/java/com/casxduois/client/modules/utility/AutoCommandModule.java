package com.casxduois.client.modules.utility;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.account.ServerAuthManager;
import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.config.StringSetting;
import com.casxduois.client.event.EventBus;
import com.casxduois.client.event.events.EventPacket;
import com.casxduois.client.event.events.EventTick;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import com.casxduois.client.util.ColorUtils;
import com.casxduois.client.util.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoCommandModule: Tính năng Auto Command / Kit tùy chỉnh thông minh.
 * - Tự động gửi lệnh nhận kit theo chu kỳ (tùy chỉnh thời gian tính bằng giây, vd: 3600s, 86400s).
 * - Kiểm tra phản hồi từ Server (Chat & Túi đồ):
 *     + Nếu nhận thành công (túi đồ tăng đồ hoặc server báo thành công): Đặt lịch hồi chiêu chính thức.
 *     + Nếu chưa đến giờ (còn cooldown hoặc chưa nhận được đồ):
 *         * Tự động bóc tách log Server báo còn bao lâu (ngày, giờ, phút, giây) để tự căn giờ chính xác.
 *         * Hoặc tự động bấm lại định kỳ theo thời gian tùy chỉnh (Retry Delay) cho đến khi nhận được kit.
 * - Hiển thị đếm ngược thời gian thực trên màn hình.
 */
public class AutoCommandModule extends Module {

    public enum State {
        READY,
        WAITING_CONFIRMATION,
        RETRYING,
        COOLDOWN
    }

    public final StringSetting command = new StringSetting("Command", "Lệnh Thực Thi (/kit)", "Lệnh gửi đến server (vd: /kit daily, /kit mem, /claim)", "/kit daily");
    public final NumberSetting cooldownSeconds = new NumberSetting("Cooldown", "Hồi Chiêu Kit (s)", "Thời gian chờ (giây) giữa các lần nhận kit thành công (vd: 3600 = 1h, 86400 = 1 ngày)", 3600.0, 1.0, 2592000.0, 5.0);
    public final NumberSetting retrySeconds = new NumberSetting("Retry Delay", "Thời Gian Bấm Lại (s)", "Số giây chờ để bấm lại khi chưa nhận được đồ hoặc server báo còn cooldown", 10.0, 1.0, 3600.0, 1.0);
    public final BooleanSetting smartCooldown = new BooleanSetting("Smart Cooldown", "Tự Bắt Cooldown Server", "Tự động đọc thời gian còn lại trong log/chat của server để căn giờ bấm lại", true);
    public final BooleanSetting checkInventory = new BooleanSetting("Check Inventory", "Kiểm Tra Túi Đồ", "Tự động xác nhận thành công khi số lượng đồ trong túi tăng lên", true);
    public final BooleanSetting showHud = new BooleanSetting("Show HUD", "Hiện Đếm Ngược Màn Hình", "Hiển thị khung đếm ngược trạng thái kit trên góc màn hình", true);
    public final StringSetting extraCommands = new StringSetting("Extra Cmds", "Lệnh Phụ Chạy Kèm", "Các lệnh phụ chạy kèm sau khi nhận kit thành công (cách nhau bởi dấu phẩy, vd: /feed, /heal)", "");

    private static final String[] SUCCESS_KEYWORDS = {
        "nhan kit", "da nhan kit", "nhan kit thanh cong", "ban da nhan kit",
        "nhan phan thuong", "da nhan phan thuong", "nhan qua thanh cong",
        "ban da nhan", "ban nhan duoc", "nhan thanh cong", "da duoc gui vao tui",
        "received kit", "you have received", "kit received", "giving you kit",
        "enjoy your kit", "kit claimed", "claimed kit", "successfully claimed"
    };

    private static final String[] COOLDOWN_KEYWORDS = {
        "ban phai cho", "vui long cho", "chua the nhan", "thoi gian hoi",
        "con lai", "hay cho them", "cooldown", "must wait", "have to wait",
        "please wait", "time left", "cannot use this kit", "kit is on cooldown"
    };

    private State state = State.READY;
    private int remainingSeconds = 0;
    private long lastSentTimestamp = 0;
    private int inventoryItemCountBefore = 0;
    private long lastSecondTick = 0;

    public AutoCommandModule() {
        super("AutoCommand", "Lệnh Tự Động / Kit", "Tự động nhận kit theo chu kỳ và tự bấm lại khi còn hồi chiêu", Category.AUTOMATION, Keyboard.KEY_NONE);
        addSetting(command);
        addSetting(cooldownSeconds);
        addSetting(retrySeconds);
        addSetting(smartCooldown);
        addSetting(checkInventory);
        addSetting(showHud);
        addSetting(extraCommands);
    }

    @Override
    public void onEnable() {
        state = State.READY;
        remainingSeconds = 0;
        lastSentTimestamp = 0;
        lastSecondTick = System.currentTimeMillis();
        sendClientMessage("§8[§bCasxDuois§8] §a✔ Đã bật Auto Command / Kit: §e" + getCleanCommand() + " §7(Chu kỳ: " + cooldownSeconds.getValueInt() + "s, Bấm lại: " + retrySeconds.getValueInt() + "s)");
    }

    @Override
    public void onDisable() {
        state = State.READY;
        remainingSeconds = 0;
    }

    @EventBus.Subscribe
    public void onTick(EventTick event) {
        if (event.getPhase() != EventTick.Phase.START || mc.player == null) return;

        long now = System.currentTimeMillis();

        // Đồng hồ đếm ngược từng giây độc lập
        if (now - lastSecondTick >= 1000) {
            lastSecondTick = now;
            if (remainingSeconds > 0) {
                remainingSeconds--;
            }

            if ((state == State.COOLDOWN || state == State.RETRYING) && remainingSeconds <= 0) {
                state = State.READY;
            }
        }

        // Khi ở trạng thái READY: Tiến hành gửi lệnh
        if (state == State.READY) {
            if (mc.currentScreen == null || mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) {
                executeKitCommand();
            }
            return;
        }

        // Khi đang chờ xác nhận: Nếu sau 2.5 giây không có phản hồi chat cooldown hay success, kiểm tra túi đồ
        if (state == State.WAITING_CONFIRMATION) {
            if (now - lastSentTimestamp >= 2500) {
                int currentCount = getTotalInventoryItemCount();
                if (checkInventory.isEnabled() && currentCount > inventoryItemCountBefore) {
                    onClaimSuccess("Túi đồ đã nhận thêm " + (currentCount - inventoryItemCountBefore) + " vật phẩm");
                } else {
                    // Không có tin nhắn chat và túi đồ không đổi -> Coi như chưa nhận được, chuyển sang thử lại định kỳ
                    remainingSeconds = retrySeconds.getValueInt();
                    state = State.RETRYING;
                    sendClientMessage("§8[§bCasxDuois§8] §6⏳ Chưa nhận được đồ từ kit. Sẽ tự động bấm lại sau §e" + remainingSeconds + "s§6...");
                    CasxDuoisClient.LOGGER.info("[AutoCommand] Chưa nhận được đồ, sẽ thử lại sau " + remainingSeconds + "s");
                }
            }
        }
    }

    private void executeKitCommand() {
        if (mc.player == null) return;
        String cmd = getCleanCommand();
        if (cmd.isEmpty()) return;

        inventoryItemCountBefore = getTotalInventoryItemCount();
        lastSentTimestamp = System.currentTimeMillis();
        state = State.WAITING_CONFIRMATION;

        mc.player.sendChatMessage(cmd);
        CasxDuoisClient.LOGGER.info("[AutoCommand] Đã gửi lệnh: " + cmd + " (Túi đồ: " + inventoryItemCountBefore + " items)");
    }

    @EventBus.Subscribe
    public void onPacket(EventPacket event) {
        if (event.getDirection() == EventPacket.Direction.INCOMING) {
            if (event.getPacket() instanceof SPacketChat) {
                SPacketChat chatPacket = (SPacketChat) event.getPacket();
                if (chatPacket.getChatComponent() != null) {
                    handleServerChat(chatPacket.getChatComponent().getUnformattedText());
                }
            } else if (event.getPacket() instanceof SPacketJoinGame) {
                // Khi vừa vào server mới, hẹn giờ 4s để kiểm tra nhận kit
                new Thread(() -> {
                    try {
                        Thread.sleep(4000);
                        mc.addScheduledTask(() -> {
                            if (isToggled() && mc.player != null) {
                                state = State.READY;
                                remainingSeconds = 0;
                            }
                        });
                    } catch (InterruptedException ignored) {}
                }, "AutoKit-Join").start();
            }
        }
    }

    private void handleServerChat(String rawMessage) {
        if (state != State.WAITING_CONFIRMATION) return;
        if (rawMessage == null || rawMessage.trim().isEmpty()) return;

        // Bỏ qua tin nhắn thông thường của người chơi khác <Player>
        if (rawMessage.matches("(?s).*<[a-zA-Z0-9_]{3,16}>.*")) {
            return;
        }

        String norm = ServerAuthManager.normalizeText(rawMessage);
        if (norm.isEmpty()) return;

        // 1. Kiểm tra nếu chat báo NHẬN THÀNH CÔNG
        if (matchesSuccess(norm)) {
            onClaimSuccess("Server xác nhận đã nhận kit thành công");
            return;
        }

        // 2. Kiểm tra nếu chat báo CÒN COOLDOWN / CHƯA ĐẾN GIỜ
        if (matchesCooldown(norm)) {
            int parsedSeconds = 0;
            if (smartCooldown.isEnabled()) {
                parsedSeconds = parseTimeFromMessage(rawMessage);
            }

            if (parsedSeconds > 0) {
                // Tự động bắt đúng thời gian server báo còn lại!
                remainingSeconds = parsedSeconds + 1; // Thêm 1 giây buffer an toàn
                state = State.COOLDOWN;
                sendClientMessage("§8[§bCasxDuois§8] §eℹ Server báo kit còn: §f" + formatTime(parsedSeconds) + "§e. Đã tự động căn giờ nhận lại!");
                CasxDuoisClient.LOGGER.info("[AutoCommand] Server báo còn hồi chiêu: " + parsedSeconds + "s, đặt lịch nhận lại");
            } else {
                // Không bắt được số giây cụ thể -> Bấm lại sau retrySeconds
                remainingSeconds = retrySeconds.getValueInt();
                state = State.RETRYING;
                sendClientMessage("§8[§bCasxDuois§8] §6⏳ Chưa nhận được kit (còn hồi chiêu). Sẽ tự động bấm lại sau §e" + remainingSeconds + "s§6...");
                CasxDuoisClient.LOGGER.info("[AutoCommand] Server báo còn hồi chiêu, thử lại sau " + remainingSeconds + "s");
            }
        }
    }

    private void onClaimSuccess(String reason) {
        state = State.COOLDOWN;
        remainingSeconds = cooldownSeconds.getValueInt();

        sendClientMessage("§8[§bCasxDuois§8] §a✔ Đã nhận kit thành công (" + reason + ")! Lần nhận tiếp theo sau: §f" + formatTime(remainingSeconds) + "§a.");
        CasxDuoisClient.LOGGER.info("[AutoCommand] Nhận kit thành công! Hẹn giờ tiếp theo sau " + remainingSeconds + "s");

        executeExtraCommands();
    }

    private void executeExtraCommands() {
        String extra = extraCommands.getValue();
        if (extra != null && !extra.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    String[] cmds = extra.split(",");
                    for (String c : cmds) {
                        String clean = c.trim();
                        if (!clean.isEmpty()) {
                            if (!clean.startsWith("/")) clean = "/" + clean;
                            String finalCmd = clean;
                            mc.addScheduledTask(() -> {
                                if (mc.player != null) {
                                    mc.player.sendChatMessage(finalCmd);
                                    CasxDuoisClient.LOGGER.info("[AutoCommand] Đã gửi lệnh phụ: " + finalCmd);
                                }
                            });
                            Thread.sleep(800);
                        }
                    }
                } catch (InterruptedException ignored) {}
            }, "AutoCmd-Extra").start();
        }
    }

    @Override
    public void onRender2D(float partialTicks) {
        if (!showHud.isEnabled() || mc.gameSettings.showDebugInfo) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRenderer;

        String cmd = getCleanCommand();
        String statusText;
        int statusColor;

        switch (state) {
            case READY:
                statusText = "§aSẵn sàng";
                statusColor = ColorUtils.ACCENT_GREEN.getRGB();
                break;
            case WAITING_CONFIRMATION:
                statusText = "§eĐang kiểm tra...";
                statusColor = ColorUtils.ACCENT_ORANGE.getRGB();
                break;
            case RETRYING:
                statusText = "§6Thử lại sau " + remainingSeconds + "s";
                statusColor = ColorUtils.ACCENT_ORANGE.getRGB();
                break;
            case COOLDOWN:
            default:
                statusText = "§f" + formatTime(remainingSeconds);
                statusColor = ColorUtils.TEXT_WHITE.getRGB();
                break;
        }

        String title = "✦ " + cmd + ": " + statusText;
        int w = fr.getStringWidth(title) + 16;
        int h = 18;
        int x = sr.getScaledWidth() - w - 8;
        int y = 56;

        RenderUtils.drawRoundedRect(x, y, w, h, 4, ColorUtils.BG_CARD.getRGB());
        RenderUtils.drawRoundedOutline(x, y, w, h, 4, 0.8f, ColorUtils.BORDER_CARD.getRGB());
        fr.drawStringWithShadow(title, x + 8, y + 5, statusColor);
    }

    private int getTotalInventoryItemCount() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.inventory.mainInventory.size(); i++) {
            ItemStack stack = mc.player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        for (int i = 0; i < mc.player.inventory.armorInventory.size(); i++) {
            ItemStack stack = mc.player.inventory.armorInventory.get(i);
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        for (int i = 0; i < mc.player.inventory.offHandInventory.size(); i++) {
            ItemStack stack = mc.player.inventory.offHandInventory.get(i);
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public String getCleanCommand() {
        String cmd = command.getValue();
        if (cmd == null || cmd.trim().isEmpty()) {
            return "/kit daily";
        }
        cmd = cmd.trim();
        if (!cmd.startsWith("/")) {
            cmd = "/" + cmd;
        }
        return cmd;
    }

    private boolean matchesSuccess(String norm) {
        for (String k : SUCCESS_KEYWORDS) {
            if (norm.contains(k)) return true;
        }
        return false;
    }

    private boolean matchesCooldown(String norm) {
        for (String k : COOLDOWN_KEYWORDS) {
            if (norm.contains(k)) return true;
        }
        return false;
    }

    public static int parseTimeFromMessage(String msg) {
        if (msg == null || msg.isEmpty()) return 0;
        String text = msg.replaceAll("(?i)[§&][0-9a-fk-or]", " ");
        String norm = ServerAuthManager.normalizeText(text);

        int totalSeconds = 0;

        Pattern dayPattern = Pattern.compile("(\\d+)\\s*(?:ngay|days|day|d)\\b");
        Matcher dayMatcher = dayPattern.matcher(norm);
        if (dayMatcher.find()) {
            try { totalSeconds += Integer.parseInt(dayMatcher.group(1)) * 86400; } catch (Exception ignored) {}
        }

        Pattern hrPattern = Pattern.compile("(\\d+)\\s*(?:gio|hours|hour|hr|h)\\b");
        Matcher hrMatcher = hrPattern.matcher(norm);
        if (hrMatcher.find()) {
            try { totalSeconds += Integer.parseInt(hrMatcher.group(1)) * 3600; } catch (Exception ignored) {}
        }

        Pattern minPattern = Pattern.compile("(\\d+)\\s*(?:phut|minutes|minute|min|m)\\b");
        Matcher minMatcher = minPattern.matcher(norm);
        if (minMatcher.find()) {
            try { totalSeconds += Integer.parseInt(minMatcher.group(1)) * 60; } catch (Exception ignored) {}
        }

        Pattern secPattern = Pattern.compile("(\\d+)\\s*(?:giay|seconds|second|sec|s)\\b");
        Matcher secMatcher = secPattern.matcher(norm);
        if (secMatcher.find()) {
            try { totalSeconds += Integer.parseInt(secMatcher.group(1)); } catch (Exception ignored) {}
        }

        return totalSeconds;
    }

    public static String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append(hours < 10 && days > 0 ? "0" : "").append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private void sendClientMessage(String msg) {
        if (mc != null) {
            mc.addScheduledTask(() -> {
                if (mc.player != null) {
                    mc.player.sendMessage(new TextComponentString(msg));
                }
            });
        }
    }

    public State getState() {
        return state;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}
