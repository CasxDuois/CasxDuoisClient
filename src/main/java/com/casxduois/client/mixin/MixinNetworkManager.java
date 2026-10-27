package com.casxduois.client.mixin;

import com.casxduois.client.CasxDuoisClient;
import com.casxduois.client.event.events.EventPacket;
import com.casxduois.client.modules.afk.AutoReconnectModule;
import com.casxduois.client.account.ServerAuthManager;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketTitle;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.casxduois.client.proxy.ProxyManager;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;

/**
 * MixinNetworkManager: Bắt gói tin mạng để phát sự kiện EventPacket, bắt phản hồi login/register từ server và định tuyến Proxy
 */
@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = {"createNetworkManagerAndConnect", "func_181124_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private static void onCreateNetworkManagerAndConnect(InetAddress address, int serverPort, boolean useNativeTransport, CallbackInfoReturnable<NetworkManager> cir) {
        if (ProxyManager.getInstance().isProxyEnabled()) {
            if (ProxyManager.getInstance().isAutoRotate()) {
                ProxyManager.getInstance().rotateNextProxy();
            }
            try {
                NetworkManager nm = ProxyManager.getInstance().connectWithProxy(address, serverPort, useNativeTransport);
                if (nm != null) {
                    cir.setReturnValue(nm);
                }
            } catch (Throwable t) {
                CasxDuoisClient.LOGGER.error("[ProxyManager] Lỗi khi kết nối qua Proxy: " + t.getMessage(), t);
            }
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), remap = false)
    private void onChannelRead0(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() != null) {
            if (CasxDuoisClient.getInstance().getEventBus() != null) {
                CasxDuoisClient.getInstance().getEventBus().post(new EventPacket(packet, EventPacket.Direction.INCOMING));
            }

            // Nhận diện thời điểm nhân vật đã spawn vào world của server
            if (packet instanceof SPacketJoinGame) {
                if (CasxDuoisClient.getInstance().getModuleManager() != null) {
                    AutoReconnectModule autoReconnect = CasxDuoisClient.getInstance().getModuleManager().getModule(AutoReconnectModule.class);
                    if (autoReconnect != null) {
                        autoReconnect.onPlayerJoinedServer();
                    }
                }
            }

            // Lắng nghe phản hồi từ server để xác nhận mật khẩu đúng/sai
            if (packet instanceof SPacketChat) {
                SPacketChat chatPacket = (SPacketChat) packet;
                if (chatPacket.getChatComponent() != null && CasxDuoisClient.getInstance().getServerAuthManager() != null) {
                    CasxDuoisClient.getInstance().getServerAuthManager().handleServerMessage(
                        chatPacket.getChatComponent().getUnformattedText(), false
                    );
                }
            } else if (packet instanceof SPacketTitle) {
                SPacketTitle titlePacket = (SPacketTitle) packet;
                if (titlePacket.getMessage() != null && CasxDuoisClient.getInstance().getServerAuthManager() != null) {
                    CasxDuoisClient.getInstance().getServerAuthManager().handleServerMessage(
                        titlePacket.getMessage().getUnformattedText(), true
                    );
                }
            }
        }
    }

    @Inject(method = {"sendPacket(Lnet/minecraft/network/Packet;)V", "func_179290_a"}, at = @At("HEAD"), remap = false)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (CasxDuoisClient.getInstance() != null && CasxDuoisClient.getInstance().getEventBus() != null) {
            CasxDuoisClient.getInstance().getEventBus().post(new EventPacket(packet, EventPacket.Direction.OUTGOING));
        }

        // Tự động bắt lệnh xác thực /login, /register, /changepassword để chờ Server xác nhận
        if (packet instanceof CPacketChatMessage) {
            String msg = ((CPacketChatMessage) packet).getMessage();
            if (msg != null && (msg.startsWith("/") || msg.startsWith("."))) {
                capturePasswordIfAuthCommand(msg);
            }
        }
    }

    private void capturePasswordIfAuthCommand(String msg) {
        try {
            ServerAuthManager authMgr = CasxDuoisClient.getInstance() != null ? CasxDuoisClient.getInstance().getServerAuthManager() : null;
            if (authMgr == null) return;

            Minecraft mc = Minecraft.getMinecraft();
            String serverIP = null;
            if (mc.getCurrentServerData() != null) {
                serverIP = mc.getCurrentServerData().serverIP;
            }
            if (serverIP == null || serverIP.isEmpty() || mc.getSession() == null) return;

            String username = mc.getSession().getUsername();
            String raw = msg.trim();
            String lower = raw.toLowerCase();

            // 1. Kiểm tra lệnh /login hoặc /l
            if (lower.startsWith("/login ") || lower.startsWith("/l ") || lower.startsWith(".login ") || lower.startsWith(".l ")) {
                String[] parts = raw.split("\\s+", 2);
                if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
                    String password = parts[1].trim();
                    // Nếu là lệnh do AutoLogin vừa gửi, giữ nguyên cờ isAutoLogin = true
                    if (!authMgr.isRecentAutoLogin(username, serverIP, password)) {
                        authMgr.recordPendingAuth(username, serverIP, password, false, "LOGIN");
                    }
                }
            }
            // 2. Kiểm tra lệnh /register hoặc /reg
            else if (lower.startsWith("/register ") || lower.startsWith("/reg ") || lower.startsWith(".register ") || lower.startsWith(".reg ")) {
                String[] parts = raw.split("\\s+");
                if (parts.length >= 3) {
                    if (parts[1].equals(parts[2])) {
                        authMgr.recordPendingAuth(username, serverIP, parts[1], false, "REGISTER");
                    } else {
                        authMgr.sendClientMessage("§8[§bCasxDuois§8] §eℹ Hai mật khẩu đăng ký không trùng khớp, vui lòng nhập lại.");
                    }
                } else if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                    authMgr.recordPendingAuth(username, serverIP, parts[1].trim(), false, "REGISTER");
                }
            }
            // 3. Kiểm tra lệnh đổi mật khẩu /changepassword hoặc /cp hoặc /doimatkhau
            else if (lower.startsWith("/changepassword ") || lower.startsWith("/cp ") || lower.startsWith("/doimatkhau ")) {
                String[] parts = raw.split("\\s+");
                if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
                    authMgr.recordPendingAuth(username, serverIP, parts[2].trim(), false, "CHANGE_PASSWORD");
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
