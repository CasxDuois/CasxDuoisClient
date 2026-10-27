package com.casxduois.client.proxy;

import com.casxduois.client.CasxDuoisClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NettyPacketDecoder;
import net.minecraft.network.NettyPacketEncoder;
import net.minecraft.network.NettyVarint21FrameDecoder;
import net.minecraft.network.NettyVarint21FrameEncoder;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.LazyLoadBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * ProxyManager: Quản lý kết nối Proxy SOCKS5, HTTP, SOCKS4 và AutoVPN quay vòng
 * Hỗ trợ định tuyến toàn bộ gói tin Minecraft qua Proxy chuẩn Netty và Java Sockets
 */
public class ProxyManager {

    private static ProxyManager instance;

    private boolean enabled = false;
    private String proxyType = "SOCKS5"; // SOCKS5, HTTP, SOCKS4
    private String host = "";
    private int port = 1080;
    private String username = "";
    private String password = "";
    private boolean autoRotate = false;
    private final List<String> proxyPool = new ArrayList<>();

    // Trạng thái kiểm tra kết nối
    private String lastTestedIp = null;
    private int lastPingMs = -1;
    private boolean isTesting = false;

    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static synchronized ProxyManager getInstance() {
        if (instance == null) {
            File dir = CasxDuoisClient.getInstance() != null ? CasxDuoisClient.getInstance().getClientDir() : new File("casxduois");
            instance = new ProxyManager(dir);
        }
        return instance;
    }

    public ProxyManager(File clientDir) {
        this.configFile = new File(clientDir, "proxy.json");
        loadConfig();
    }

    public boolean isProxyEnabled() {
        return enabled && host != null && !host.trim().isEmpty() && port > 0;
    }

    public void setProxyEnabled(boolean enabled) {
        this.enabled = enabled;
        applyJavaProperties();
        saveConfig();
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
        applyJavaProperties();
        saveConfig();
    }

    public String getHost() {
        return host != null ? host : "";
    }

    public void setHost(String host) {
        this.host = host != null ? host.trim() : "";
        applyJavaProperties();
        saveConfig();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        applyJavaProperties();
        saveConfig();
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : "";
        applyJavaProperties();
        saveConfig();
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public void setPassword(String password) {
        this.password = password != null ? password : "";
        applyJavaProperties();
        saveConfig();
    }

    public boolean isAutoRotate() {
        return autoRotate;
    }

    public void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
        saveConfig();
    }

    public List<String> getProxyPool() {
        return proxyPool;
    }

    public String getLastTestedIp() {
        return lastTestedIp;
    }

    public int getLastPingMs() {
        return lastPingMs;
    }

    public boolean isTesting() {
        return isTesting;
    }

    /**
     * Đồng bộ biến môi trường Java Sockets để các luồng HTTP/Session cũng đi qua Proxy
     */
    public void applyJavaProperties() {
        try {
            if (isProxyEnabled()) {
                if ("HTTP".equalsIgnoreCase(proxyType)) {
                    System.setProperty("http.proxyHost", host.trim());
                    System.setProperty("http.proxyPort", String.valueOf(port));
                    System.setProperty("https.proxyHost", host.trim());
                    System.setProperty("https.proxyPort", String.valueOf(port));
                    System.clearProperty("socksProxyHost");
                    System.clearProperty("socksProxyPort");
                } else {
                    System.setProperty("socksProxyHost", host.trim());
                    System.setProperty("socksProxyPort", String.valueOf(port));
                    System.clearProperty("http.proxyHost");
                    System.clearProperty("http.proxyPort");
                    System.clearProperty("https.proxyHost");
                    System.clearProperty("https.proxyPort");
                }

                if (username != null && !username.trim().isEmpty()) {
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username.trim(), password != null ? password.toCharArray() : new char[0]);
                        }
                    });
                } else {
                    Authenticator.setDefault(null);
                }
            } else {
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
                Authenticator.setDefault(null);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Tạo Netty ProxyHandler gắn vào kênh pipeline trước khi kết nối vào server
     */
    public ProxyHandler createNettyProxyHandler() {
        if (!isProxyEnabled()) return null;

        try {
            InetSocketAddress proxyAddr = new InetSocketAddress(host.trim(), port);
            boolean hasAuth = username != null && !username.trim().isEmpty();

            if ("HTTP".equalsIgnoreCase(proxyType) || "HTTPS".equalsIgnoreCase(proxyType) || "HTTP/HTTPS".equalsIgnoreCase(proxyType)) {
                if (hasAuth) {
                    return new HttpProxyHandler(proxyAddr, username.trim(), password != null ? password : "");
                } else {
                    return new HttpProxyHandler(proxyAddr);
                }
            } else if ("SOCKS4".equalsIgnoreCase(proxyType)) {
                if (hasAuth) {
                    return new Socks4ProxyHandler(proxyAddr, username.trim());
                } else {
                    return new Socks4ProxyHandler(proxyAddr);
                }
            } else {
                // SOCKS5 chuẩn (Hỗ trợ UDP, TCP cho Minecraft)
                if (hasAuth) {
                    return new Socks5ProxyHandler(proxyAddr, username.trim(), password != null ? password : "");
                } else {
                    return new Socks5ProxyHandler(proxyAddr);
                }
            }
        } catch (Throwable t) {
            CasxDuoisClient.LOGGER.error("[ProxyManager] Không thể tạo ProxyHandler: " + t.getMessage());
            return null;
        }
    }

    /**
     * Kết nối Minecraft NetworkManager qua Netty với ProxyHandler tích hợp
     */
    public NetworkManager connectWithProxy(InetAddress address, int serverPort, boolean useNativeTransport) {
        if (address instanceof java.net.Inet6Address) {
            System.setProperty("java.net.preferIPv4Stack", "false");
        }
        final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
        Class<? extends SocketChannel> oclass;
        LazyLoadBase<? extends EventLoopGroup> lazyloadbase;

        if (Epoll.isAvailable() && useNativeTransport) {
            oclass = EpollSocketChannel.class;
            lazyloadbase = NetworkManager.CLIENT_EPOLL_EVENTLOOP;
        } else {
            oclass = NioSocketChannel.class;
            lazyloadbase = NetworkManager.CLIENT_NIO_EVENTLOOP;
        }

        final ProxyHandler proxyHandler = createNettyProxyHandler();
        CasxDuoisClient.LOGGER.info(String.format("✔ [ProxyManager] Đang định tuyến kết nối qua Proxy %s (%s:%d) tới server %s:%d",
                proxyType, host, port, address.getHostAddress(), serverPort));

        (new Bootstrap()).group(lazyloadbase.getValue()).handler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) throws Exception {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.valueOf(true));
                } catch (ChannelException ignored) {}

                if (proxyHandler != null) {
                    channel.pipeline().addFirst("casxduois_proxy", proxyHandler);
                }

                channel.pipeline()
                        .addLast("timeout", new ReadTimeoutHandler(30))
                        .addLast("splitter", new NettyVarint21FrameDecoder())
                        .addLast("decoder", new NettyPacketDecoder(EnumPacketDirection.CLIENTBOUND))
                        .addLast("prepender", new NettyVarint21FrameEncoder())
                        .addLast("encoder", new NettyPacketEncoder(EnumPacketDirection.SERVERBOUND))
                        .addLast("packet_handler", networkmanager);
            }
        }).channel(oclass).connect(address, serverPort).syncUninterruptibly();

        return networkmanager;
    }

    /**
     * Tự động chuyển sang Proxy tiếp theo trong danh sách quay vòng (Auto-Rotate)
     */
    public boolean rotateNextProxy() {
        if (proxyPool.isEmpty()) return false;
        Random rng = new Random();
        int idx = rng.nextInt(proxyPool.size());
        String entry = proxyPool.get(idx);
        return parseAndSetProxy(entry);
    }

    public boolean parseAndSetProxy(String raw) {
        if (raw == null || raw.trim().isEmpty()) return false;
        try {
            String clean = raw.trim();
            if (clean.toLowerCase().startsWith("socks5://")) {
                this.proxyType = "SOCKS5";
                clean = clean.substring("socks5://".length());
            } else if (clean.toLowerCase().startsWith("socks4://")) {
                this.proxyType = "SOCKS4";
                clean = clean.substring("socks4://".length());
            } else if (clean.toLowerCase().startsWith("http://")) {
                this.proxyType = "HTTP/HTTPS";
                clean = clean.substring("http://".length());
            } else if (clean.toLowerCase().startsWith("https://")) {
                this.proxyType = "HTTP/HTTPS";
                clean = clean.substring("https://".length());
            }
            String[] parts = clean.split(":");
            if (parts.length >= 2) {
                this.host = parts[0].trim();
                this.port = Integer.parseInt(parts[1].trim());
                if (parts.length >= 4) {
                    this.username = parts[2].trim();
                    this.password = parts[3].trim();
                } else {
                    this.username = "";
                    this.password = "";
                }
                applyJavaProperties();
                saveConfig();
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Tự động tìm kiếm Proxy Việt Nam miễn phí có ping thấp nhất và áp dụng vào Client
     */
    public void autoSetupVietnamProxy(boolean enableNow, Consumer<VietnamProxyService.VNProxyResult> callback) {
        VietnamProxyService.fetchAndFindBestVNProxyAsync(result -> {
            if (result.success && result.bestProxy != null) {
                this.host = result.bestProxy.ip;
                this.port = result.bestProxy.port;
                this.proxyType = result.bestProxy.type;
                this.username = "";
                this.password = "";
                this.lastTestedIp = result.bestProxy.ip;
                this.lastPingMs = result.bestProxy.pingMs;

                // Tự động thêm tất cả các proxy VN sống vào Proxy Pool quay vòng
                if (result.aliveList != null) {
                    for (VietnamProxyService.VNProxyItem item : result.aliveList) {
                        String entry = item.ip + ":" + item.port;
                        if (!proxyPool.contains(entry)) {
                            proxyPool.add(entry);
                        }
                    }
                }

                if (enableNow) {
                    this.enabled = true;
                    applyJavaProperties();
                }
                saveConfig();
            }

            if (callback != null) {
                callback.accept(result);
            }
        });
    }

    /**
     * Kiểm tra tốc độ và địa chỉ IP công khai của Proxy trong luồng ngầm
     */
    public void testProxyAsync(Consumer<String> callback) {
        if (isTesting) return;
        isTesting = true;
        lastTestedIp = "Đang kiểm tra...";
        lastPingMs = -1;

        new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                Proxy proxy;
                if (!isProxyEnabled()) {
                    proxy = Proxy.NO_PROXY;
                } else {
                    boolean isHttpType = "HTTP".equalsIgnoreCase(proxyType) || "HTTPS".equalsIgnoreCase(proxyType) || "HTTP/HTTPS".equalsIgnoreCase(proxyType);
                    Proxy.Type type = isHttpType ? Proxy.Type.HTTP : Proxy.Type.SOCKS;
                    proxy = new Proxy(type, new InetSocketAddress(host.trim(), port));
                }

                URL url = new URL("http://checkip.amazonaws.com");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                int code = conn.getResponseCode();
                long duration = System.currentTimeMillis() - start;
                if (code == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String ip = reader.readLine();
                        lastTestedIp = ip != null ? ip.trim() : "Thành công";
                        lastPingMs = (int) duration;
                    }
                } else {
                    lastTestedIp = "Lỗi HTTP: " + code;
                    lastPingMs = (int) duration;
                }
                conn.disconnect();
            } catch (Exception e) {
                lastTestedIp = "Lỗi: " + (e.getMessage() != null ? e.getMessage() : "Timeout");
                lastPingMs = -1;
            } finally {
                isTesting = false;
                if (callback != null) {
                    callback.accept(lastTestedIp);
                }
            }
        }, "Proxy-Tester").start();
    }

    public void saveConfig() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }

            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            root.addProperty("proxyType", proxyType);
            root.addProperty("host", host);
            root.addProperty("port", port);
            root.addProperty("username", username);
            root.addProperty("password", password);
            root.addProperty("autoRotate", autoRotate);

            JsonArray poolArray = new JsonArray();
            for (String p : proxyPool) {
                poolArray.add(p);
            }
            root.add("proxyPool", poolArray);

            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
            if (root.has("proxyType")) proxyType = root.get("proxyType").getAsString();
            if (root.has("host")) host = root.get("host").getAsString();
            if (root.has("port")) port = root.get("port").getAsInt();
            if (root.has("username")) username = root.get("username").getAsString();
            if (root.has("password")) password = root.get("password").getAsString();
            if (root.has("autoRotate")) autoRotate = root.get("autoRotate").getAsBoolean();

            proxyPool.clear();
            if (root.has("proxyPool")) {
                JsonArray poolArray = root.getAsJsonArray("proxyPool");
                for (JsonElement el : poolArray) {
                    proxyPool.add(el.getAsString());
                }
            }
            applyJavaProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
