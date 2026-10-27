package com.casxduois.client;

import com.casxduois.client.account.AccountManager;
import com.casxduois.client.account.ServerAuthManager;
import com.casxduois.client.config.ConfigManager;
import com.casxduois.client.core.MemoryOptimizer;
import com.casxduois.client.core.MultiTabManager;
import com.casxduois.client.discord.DiscordRESTPoller;
import com.casxduois.client.event.EventBus;
import com.casxduois.client.event.events.EventDisconnect;
import com.casxduois.client.event.events.EventKey;
import com.casxduois.client.event.events.EventRender2D;
import com.casxduois.client.event.events.EventTick;
import com.casxduois.client.gui.clickgui.CasxDuoisClickGui;
import com.casxduois.client.gui.hud.CasxDuoisHUD;
import com.casxduois.client.modules.Module;
import com.casxduois.client.modules.ModuleManager;
import com.casxduois.client.modules.afk.AutoReconnectModule;
import com.casxduois.client.modules.external.ExternalModManager;
import com.casxduois.client.modules.utility.DiscordRemoteModule;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;

/**
 * CasxDuois Client Core: Trọng tâm kiến trúc quản lý toàn bộ hệ thống Standalone Client
 */
public class CasxDuoisClient {

    public static final String CLIENT_NAME = "CasxDuois Client";
    public static final String CLIENT_VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger("CasxDuoisClient");

    private static CasxDuoisClient instance;

    private EventBus eventBus;
    private AccountManager accountManager;
    private ServerAuthManager serverAuthManager;
    private MultiTabManager multiTabManager;
    private MemoryOptimizer memoryOptimizer;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private DiscordRESTPoller discordPoller;
    private ExternalModManager externalModManager;
    private CasxDuoisHUD hud;
    private CasxDuoisClickGui clickGui;

    private File clientDir;

    public static CasxDuoisClient getInstance() {
        if (instance == null) {
            instance = new CasxDuoisClient();
        }
        return instance;
    }

    public void init() {
        LOGGER.info("Đang khởi tạo " + CLIENT_NAME + " phiên bản " + CLIENT_VERSION + "...");

        Minecraft mc = Minecraft.getMinecraft();
        this.clientDir = new File(mc.gameDir, "casxduois");
        if (!this.clientDir.exists()) {
            this.clientDir.mkdirs();
        }

        this.eventBus = new EventBus();
        this.accountManager = new AccountManager(this.clientDir);
        this.serverAuthManager = new ServerAuthManager(this.clientDir);
        this.multiTabManager = new MultiTabManager();
        this.memoryOptimizer = new MemoryOptimizer();
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager(this.clientDir);
        this.discordPoller = new DiscordRESTPoller(this.clientDir);
        this.externalModManager = new ExternalModManager();
        this.hud = new CasxDuoisHUD();
        this.clickGui = new CasxDuoisClickGui();

        // Nạp cấu hình đã lưu
        this.configManager.loadConfig();

        // Đồng bộ cấu hình DiscordRemoteModule nếu có
        DiscordRemoteModule discordMod = this.moduleManager.getModule(DiscordRemoteModule.class);
        if (discordMod != null && discordMod.isToggled()) {
            discordMod.syncPollerSettings();
            this.discordPoller.start();
        }

        // Đăng ký nhận Event cốt lõi
        this.eventBus.register(this);
        try {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        } catch (Throwable ignored) {
        }

        // Tự động kiểm tra bản cập nhật mới trên GitHub Releases
        com.casxduois.client.update.UpdateManager.getInstance().checkUpdatesAsync();

        LOGGER.info(CLIENT_NAME + " đã sẵn sàng! Chúc bạn trải nghiệm mượt mà và treo máy hiệu quả.");
    }

    public void shutdown() {
        LOGGER.info("Đang lưu cấu hình và tắt " + CLIENT_NAME + "...");
        if (this.discordPoller != null) {
            this.discordPoller.stop();
        }
        if (this.serverAuthManager != null) {
            this.serverAuthManager.save();
        }
        if (this.configManager != null) {
            this.configManager.saveConfig();
        }
        if (this.accountManager != null) {
            this.accountManager.saveAccounts();
        }
    }

    private boolean rshiftWasDown = false;

    @EventBus.Subscribe
    public void onTick(EventTick event) {
        if (event.getPhase() == EventTick.Phase.START) {
            // Thông báo cập nhật trong chat nếu có bản mới khi vào game
            com.casxduois.client.update.UpdateManager.getInstance().notifyInChatIfEligible();

            // Cơ chế Edge-Detection bấm RIGHT SHIFT thời gian thực (bảo đảm 100% nhận diện cả khi không có Event)
            try {
                if (Keyboard.isCreated()) {
                    boolean rshiftNow = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
                    if (rshiftNow && !rshiftWasDown) {
                        Minecraft mc = Minecraft.getMinecraft();
                        if (mc.currentScreen == null) {
                            mc.displayGuiScreen(this.clickGui);
                        }
                    }
                    rshiftWasDown = rshiftNow;
                }
            } catch (Throwable ignored) {
            }

            if (this.memoryOptimizer != null) {
                this.memoryOptimizer.tick();
            }

            if (this.moduleManager != null) {
                for (Module m : this.moduleManager.getModules()) {
                    if (m.isToggled()) {
                        m.onTick();
                    }
                }
            }
        }
    }

    @EventBus.Subscribe
    public void onRender2D(EventRender2D event) {
        if (this.hud != null) {
            this.hud.render(event.getPartialTicks());
        }

        if (this.moduleManager != null) {
            for (Module m : this.moduleManager.getModules()) {
                if (m.isToggled()) {
                    m.onRender2D(event.getPartialTicks());
                }
            }
        }
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onForgeKeyInput(net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent event) {
        try {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
                this.onKey(new EventKey(key));
            }
        } catch (Throwable ignored) {
        }
    }

    @EventBus.Subscribe
    public void onKey(EventKey event) {
        if (event.getKey() == Keyboard.KEY_NONE) return;

        // Cập nhật trạng thái hoạt động của người dùng
        if (this.multiTabManager != null) {
            this.multiTabManager.updateUserInput();
        }

        // Mở ClickGUI bằng phím RIGHT SHIFT
        com.casxduois.client.modules.render.ClickGUIModule clickGUIMod = this.moduleManager != null ? this.moduleManager.getModule(com.casxduois.client.modules.render.ClickGUIModule.class) : null;
        int targetKey = (clickGUIMod != null && clickGUIMod.getKey() != Keyboard.KEY_NONE) ? clickGUIMod.getKey() : Keyboard.KEY_RSHIFT;

        if (event.getKey() == Keyboard.KEY_RSHIFT || event.getKey() == targetKey) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null || mc.currentScreen instanceof com.casxduois.client.gui.menu.CasxDuoisMainMenu) {
                mc.displayGuiScreen(this.clickGui);
            }
            return;
        }

        if (this.moduleManager != null) {
            this.moduleManager.onKey(event.getKey());
        }
    }

    @EventBus.Subscribe
    public void onDisconnect(EventDisconnect event) {
        // Lưu lại server gần nhất để Auto-Reconnect
        AutoReconnectModule autoReconnect = this.moduleManager.getModule(AutoReconnectModule.class);
        if (autoReconnect != null && autoReconnect.isToggled()) {
            autoReconnect.setLastServerData(Minecraft.getMinecraft().getCurrentServerData());
        }
        if (this.serverAuthManager != null) {
            this.serverAuthManager.clearPendingAuth();
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public ServerAuthManager getServerAuthManager() {
        return serverAuthManager;
    }

    public MultiTabManager getMultiTabManager() {
        return multiTabManager;
    }

    public MemoryOptimizer getMemoryOptimizer() {
        return memoryOptimizer;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordRESTPoller getDiscordPoller() {
        return discordPoller;
    }

    public ExternalModManager getExternalModManager() {
        return externalModManager;
    }

    public CasxDuoisHUD getHud() {
        return hud;
    }

    public CasxDuoisClickGui getClickGui() {
        return clickGui;
    }

    public File getClientDir() {
        return clientDir;
    }
}
