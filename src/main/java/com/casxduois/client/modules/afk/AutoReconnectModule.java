package com.casxduois.client.modules.afk;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.config.BooleanSetting;
import com.casxduois.client.config.NumberSetting;
import com.casxduois.client.config.StringSetting;
import com.casxduois.client.modules.Category;
import com.casxduois.client.modules.Module;
import net.minecraft.client.multiplayer.ServerData;
import org.lwjgl.input.Keyboard;

/**
 * AutoReconnectModule: Tự động kết nối lại khi mất mạng và tự động đăng nhập /login đa server theo IP
 */
public class AutoReconnectModule extends Module {

    public final NumberSetting delay = new NumberSetting("Delay", "Thời Gian Chờ (s)", "Số giây đếm ngược trước khi tự động vào lại", 5.0, 1.0, 60.0, 1.0);
    public final BooleanSetting autoLogin = new BooleanSetting("Auto Login", "Tự Động Đăng Nhập", "Tự động gửi /login <mk> đúng theo IP server sau khi vào game", true);
    public final NumberSetting loginDelay = new NumberSetting("Login Delay", "Độ Trễ Login (s)", "Thời gian chờ sau khi spawn để server sẵn sàng nhận lệnh", 2.0, 1.0, 5.0, 0.5);
    public final StringSetting serverPassword = new StringSetting("Server Pass", "Mật Khẩu Server Này", "Mật khẩu login của server hiện tại (tự động lưu khi gõ /login)", "", true);
    public final StringSetting customCommand = new StringSetting("Post Command", "Lệnh Chuyển Sảnh", "Lệnh gửi sau khi đăng nhập thành công (ví dụ /is, /server s1)", "");

    private ServerData lastServerData;

    public AutoReconnectModule() {
        super("AutoReconnect", "Tự Vào Lại Server", "Tự kết nối lại khi mất mạng và tự động đăng nhập tài khoản", null, Keyboard.KEY_NONE);
        addSetting(delay);
        addSetting(autoLogin);
        addSetting(loginDelay);
        addSetting(serverPassword);
        addSetting(customCommand);
        setToggled(true);
    }

    public ServerData getLastServerData() {
        return lastServerData;
    }

    public void setLastServerData(ServerData lastServerData) {
        if (lastServerData != null) {
            this.lastServerData = lastServerData;
        }
    }

    public void syncCurrentServerPassword() {
        ServerData sData = lastServerData != null ? lastServerData : mc.getCurrentServerData();
        if (sData != null && mc.getSession() != null && CasxDuoisClient.getInstance().getServerAuthManager() != null) {
            String username = mc.getSession().getUsername();
            String serverIP = sData.serverIP;
            String pass = CasxDuoisClient.getInstance().getServerAuthManager().getPassword(username, serverIP);
            serverPassword.setValue(pass != null ? pass : "");
        } else {
            serverPassword.setValue("");
        }
    }

    public void onServerPasswordChanged() {
        ServerData sData = lastServerData != null ? lastServerData : mc.getCurrentServerData();
        if (sData != null && mc.getSession() != null && CasxDuoisClient.getInstance().getServerAuthManager() != null) {
            String username = mc.getSession().getUsername();
            String serverIP = sData.serverIP;
            CasxDuoisClient.getInstance().getServerAuthManager().setPassword(username, serverIP, serverPassword.getValue());
            CasxDuoisClient.LOGGER.info("✔ [ServerAuth] Đã cập nhật mật khẩu cho " + username + " tại " + serverIP);
        }
    }

    private volatile boolean postCommandExecuted = false;

    /**
     * Kích hoạt khi nhận packet SPacketJoinGame (nhân vật vừa spawn hoàn tất trên server)
     */
    public void onPlayerJoinedServer() {
        if (!isToggled() || !autoLogin.isEnabled()) {
            return;
        }

        postCommandExecuted = false;

        new Thread(() -> {
            try {
                // Đợi thời gian quy định để plugin AuthMe/LoginSecurity trên server sẵn sàng
                Thread.sleep((long) (loginDelay.getValue() * 1000));

                mc.addScheduledTask(() -> {
                    if (mc.player == null) return;

                    ServerData sData = lastServerData != null ? lastServerData : mc.getCurrentServerData();
                    if (sData == null) return;

                    String username = mc.getSession().getUsername();
                    String serverIP = sData.serverIP;

                    syncCurrentServerPassword();

                    if (CasxDuoisClient.getInstance().getServerAuthManager() != null) {
                        String password = CasxDuoisClient.getInstance().getServerAuthManager().getPassword(username, serverIP);
                        if (password != null && !password.trim().isEmpty()) {
                            // Ghi nhận lượt AutoLogin đang chờ Server xác nhận
                            CasxDuoisClient.getInstance().getServerAuthManager().recordPendingAuth(username, serverIP, password.trim(), true, "AUTO_LOGIN");
                            mc.player.sendChatMessage("/login " + password.trim());
                            CasxDuoisClient.LOGGER.info("✔ [AutoLogin] Đã tự động gửi /login cho " + username + " tại " + serverIP);
                        } else {
                            CasxDuoisClient.LOGGER.info("ℹ [AutoLogin] Server " + serverIP + " chưa có mật khẩu đã lưu cho nick " + username + ", bỏ qua login.");
                        }
                    }
                });
            } catch (InterruptedException ignored) {
            }
        }, "AutoLogin-Thread").start();
    }

    /**
     * Kích hoạt khi Server đã xác nhận mật khẩu đúng và đăng nhập thành công
     */
    public void onLoginSuccess() {
        if (postCommandExecuted) return;
        postCommandExecuted = true;

        String postCmd = customCommand.getValue();
        if (postCmd != null && !postCmd.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    // Chờ 800ms để server hoàn tất quá trình teleport/khởi tạo người chơi sau khi auth
                    Thread.sleep(800);
                    mc.addScheduledTask(() -> {
                        if (mc.player != null) {
                            String cmd = postCmd.trim();
                            if (!cmd.startsWith("/")) cmd = "/" + cmd;
                            mc.player.sendChatMessage(cmd);
                            CasxDuoisClient.LOGGER.info("✔ [AutoLogin] Đã gửi lệnh sau khi đăng nhập thành công: " + cmd);
                        }
                    });
                } catch (InterruptedException ignored) {
                }
            }, "PostLoginCmd-Thread").start();
        }
    }
}
