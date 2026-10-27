package com.casxduois.client.proxy;

import com.casxduois.client.CasxDuoisClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * VietnamProxyService: Dịch vụ tự động tìm kiếm, lọc và kiểm tra tốc độ (Ping)
 * các máy chủ Proxy SOCKS5 / HTTP tại Việt Nam (VN) hoàn toàn miễn phí
 */
public class VietnamProxyService {

    public static class VNProxyItem {
        public String ip;
        public int port;
        public String type; // SOCKS5, HTTP, SOCKS4
        public int pingMs;

        public VNProxyItem(String ip, int port, String type) {
            this.ip = ip;
            this.port = port;
            this.type = type;
            this.pingMs = 9999;
        }

        public String toFormatString() {
            return ip + ":" + port;
        }
    }

    public static class VNProxyResult {
        public boolean success;
        public VNProxyItem bestProxy;
        public List<VNProxyItem> aliveList = new ArrayList<>();
        public String message;

        public VNProxyResult(boolean success, VNProxyItem bestProxy, List<VNProxyItem> aliveList, String message) {
            this.success = success;
            this.bestProxy = bestProxy;
            this.aliveList = aliveList != null ? aliveList : new ArrayList<>();
            this.message = message;
        }
    }

    // Danh sách dự phòng các dải IP Việt Nam ổn định
    private static final String[] FALLBACK_VN_PROXIES = {
            "118.71.227.154:1095:SOCKS5",
            "171.245.71.217:1105:SOCKS5",
            "118.71.227.154:1057:SOCKS5",
            "118.71.227.154:1081:SOCKS5",
            "171.252.130.172:4001:HTTP",
            "118.71.227.154:1108:SOCKS5",
            "118.71.227.154:1044:SOCKS5",
            "118.71.227.154:1034:SOCKS5",
            "116.105.27.177:8080:HTTP",
            "14.225.240.23:8562:HTTP"
    };

    /**
     * Tự động quét và tìm Proxy Việt Nam tốt nhất trong luồng ngầm
     */
    public static void fetchAndFindBestVNProxyAsync(Consumer<VNProxyResult> callback) {
        new Thread(() -> {
            CasxDuoisClient.LOGGER.info("[VietnamProxy] Đang bắt đầu quét Proxy Việt Nam miễn phí...");
            Set<String> uniqueKeys = new HashSet<>();
            List<VNProxyItem> candidates = new ArrayList<>();

            // 1. Quét từ Proxyscrape SOCKS5 Việt Nam
            scrapeProxyscrape("https://api.proxyscrape.com/v2/?request=displayproxies&protocol=socks5&timeout=10000&country=VN&ssl=all&anonymity=all", "SOCKS5", candidates, uniqueKeys);

            // 2. Quét từ Proxyscrape HTTP Việt Nam
            scrapeProxyscrape("https://api.proxyscrape.com/v2/?request=displayproxies&protocol=http&timeout=10000&country=VN&ssl=all&anonymity=all", "HTTP", candidates, uniqueKeys);

            // 3. Quét từ Geonode API Việt Nam
            scrapeGeonode(candidates, uniqueKeys);

            // 4. Nếu không lấy được hoặc danh sách quá ít, nạp thêm từ danh sách dự phòng
            if (candidates.size() < 5) {
                for (String raw : FALLBACK_VN_PROXIES) {
                    String[] p = raw.split(":");
                    if (p.length >= 3) {
                        String key = p[0] + ":" + p[1];
                        if (uniqueKeys.add(key)) {
                            try {
                                candidates.add(new VNProxyItem(p[0], Integer.parseInt(p[1]), p[2]));
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            CasxDuoisClient.LOGGER.info(String.format("[VietnamProxy] Đã tìm thấy %d ứng viên Proxy VN. Đang kiểm tra Ping đa luồng...", candidates.size()));

            if (candidates.isEmpty()) {
                if (callback != null) {
                    callback.accept(new VNProxyResult(false, null, null, "Không tìm thấy Proxy VN khả dụng"));
                }
                return;
            }

            // 5. Kiểm tra kết nối song song (Multi-threaded Ping Checker)
            ExecutorService executor = Executors.newFixedThreadPool(12);
            List<VNProxyItem> aliveList = Collections.synchronizedList(new ArrayList<>());
            List<Future<?>> futures = new ArrayList<>();

            // Giới hạn kiểm tra 35 proxy đầu để đảm bảo phản hồi trong 2 - 3 giây
            int testLimit = Math.min(candidates.size(), 35);
            for (int i = 0; i < testLimit; i++) {
                final VNProxyItem item = candidates.get(i);
                futures.add(executor.submit(() -> {
                    try (Socket socket = new Socket()) {
                        long start = System.currentTimeMillis();
                        socket.connect(new InetSocketAddress(item.ip, item.port), 2200);
                        int ping = (int) (System.currentTimeMillis() - start);
                        item.pingMs = ping;
                        aliveList.add(item);
                    } catch (Throwable ignored) {
                        // Proxy không phản hồi hoặc timeout
                    }
                }));
            }

            // Chờ tất cả luồng hoàn tất
            for (Future<?> f : futures) {
                try {
                    f.get(2600, TimeUnit.MILLISECONDS);
                } catch (Exception ignored) {}
            }
            executor.shutdownNow();

            if (aliveList.isEmpty()) {
                CasxDuoisClient.LOGGER.warn("[VietnamProxy] Không có Proxy VN nào phản hồi trực tiếp");
                if (callback != null) {
                    callback.accept(new VNProxyResult(false, null, null, "Các Proxy VN hiện tại bị quá tải hoặc Timeout"));
                }
                return;
            }

            // 6. Sắp xếp theo Ping nhanh nhất
            aliveList.sort(Comparator.comparingInt(a -> a.pingMs));
            VNProxyItem best = aliveList.get(0);

            CasxDuoisClient.LOGGER.info(String.format("✔ [VietnamProxy] Đã chọn Proxy VN tốt nhất: %s (%s:%d) - Ping: %dms (Tổng cộng %d Proxy sống)",
                    best.type, best.ip, best.port, best.pingMs, aliveList.size()));

            if (callback != null) {
                String msg = String.format("Thành công: %s:%d (%dms) - Tìm thấy %d Proxy VN sống",
                        best.ip, best.port, best.pingMs, aliveList.size());
                callback.accept(new VNProxyResult(true, best, aliveList, msg));
            }
        }, "VNProxy-Finder").start();
    }

    private static void scrapeProxyscrape(String urlStr, String type, List<VNProxyItem> list, Set<String> uniqueKeys) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(3500);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.contains(":")) {
                            String[] parts = line.split(":");
                            if (parts.length >= 2) {
                                String key = parts[0].trim() + ":" + parts[1].trim();
                                if (uniqueKeys.add(key)) {
                                    try {
                                        list.add(new VNProxyItem(parts[0].trim(), Integer.parseInt(parts[1].trim()), type));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Throwable t) {
            CasxDuoisClient.LOGGER.warn("[VietnamProxy] Lỗi đọc Proxyscrape " + type + ": " + t.getMessage());
        }
    }

    private static void scrapeGeonode(List<VNProxyItem> list, Set<String> uniqueKeys) {
        try {
            String urlStr = "https://proxylist.geonode.com/api/proxy-list?country=VN&limit=30&page=1&sort_by=lastChecked&sort_type=desc";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(3500);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                    if (root.has("data")) {
                        JsonArray dataArr = root.getAsJsonArray("data");
                        for (JsonElement el : dataArr) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                String ip = obj.has("ip") ? obj.get("ip").getAsString() : null;
                                int port = obj.has("port") ? obj.get("port").getAsInt() : -1;
                                String proto = "SOCKS5";
                                if (obj.has("protocols") && obj.get("protocols").isJsonArray()) {
                                    JsonArray pArr = obj.getAsJsonArray("protocols");
                                    if (pArr.size() > 0) {
                                        proto = pArr.get(0).getAsString().toUpperCase();
                                    }
                                }
                                if (ip != null && port > 0) {
                                    String key = ip + ":" + port;
                                    if (uniqueKeys.add(key)) {
                                        list.add(new VNProxyItem(ip, port, proto));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Throwable t) {
            CasxDuoisClient.LOGGER.warn("[VietnamProxy] Lỗi đọc Geonode VN: " + t.getMessage());
        }
    }
}
