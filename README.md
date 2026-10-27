# 🌌 CasxDuois Client
### *Minecraft 1.12.2 · Monochrome Edition · Motion Blur & High Performance*

![Version](https://img.shields.io/badge/Version-v1.0.0-black?style=for-the-badge&logo=github)
![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-white?style=for-the-badge&logo=minecraft)
![Forge](https://img.shields.io/badge/Forge-14.23.5.2860-lightgrey?style=for-the-badge)
![Theme](https://img.shields.io/badge/Theme-Monochrome_Obsidian-08090D?style=for-the-badge)
![Motion Blur](https://img.shields.io/badge/Motion__Blur-240Hz_Cinematic-white?style=for-the-badge)

---

**CasxDuois Client** là giải pháp Minecraft Client thế hệ mới, tối ưu hóa cho **trải nghiệm PvP mượt mà với hiệu ứng Motion Blur 240Hz**, độ trễ thấp và giao diện tối giản tinh tế.

Toàn bộ ứng dụng được thiết kế theo phong cách **Monochrome Obsidian & White** với hiệu ứng kính mờ **Glassmorphism** và chuyển động **Kinetic Motion Blur**.

---

## 📁 Cấu trúc dự án

```
d:/CasxDuois/
├── 🚀 CasxDuoisLauncher.exe       # Trình khởi chạy Windows Native (Mở trực tiếp)
├── ☕ CasxDuoisLauncher.jar       # Trình khởi chạy Cross-Platform (Java Swing)
├── ⚡ run_launcher.bat            # Phím tắt mở nhanh Launcher
├── 🎨 app.ico                    # Icon ứng dụng (Monochrome Squircle)
│
├── 🌐 app_ui/                    # Giao diện Web Client Frontend & Backend API
│   ├── index.html                # UI Client phong cách kính mờ & chuyển động mượt
│   ├── style.css                 # Hệ màu Monochrome Obsidian & White
│   ├── app.js                    # Bộ điều khiển logic ứng dụng
│   ├── server.js                 # Local API Server điều phối Minecraft & tài khoản
│   └── logo.png                  # Biểu tượng chính thức của ứng dụng
│
├── 📦 build/libs/                # Mod Client JAR đã biên dịch & reobfuscate
│   └── CasxDuoisMCClient-1.0.0-LITE.jar
│
├── 💻 src/                       # Toàn bộ mã nguồn mở
│   ├── launcher_native/          # Mã nguồn C# WPF Launcher (CasxDuoisLauncherApp.cs)
│   └── main/java/com/casxduois/  # Mã nguồn Mod Client (Mixins, Modules, Gui, Launcher)
│
└── 📄 README.md                  # Hướng dẫn sử dụng & phát hành
```

---

## ⚡ Tính năng nổi bật

### 1. 🎬 Cinematic Motion Blur (240Hz)
* **Khử bóng ma & xé hình:** Tự động hòa trộn khung hình mượt mà qua shader OpenGL, đem lại cảm giác di chuyển và combat mềm mại như trên màn hình 240Hz.
* **Không tụt FPS:** Xử lý trực tiếp trên GPU, không chiếm dụng tài nguyên CPU.
* **Cách kích hoạt:** Bấm phím **`Right Shift`** trong game $\rightarrow$ Tab **Render** $\rightarrow$ Bật **MotionBlur**.

### 2. 🪟 Frosted Glassmorphism UI
* Giao diện Client Desktop và menu in-game đồng nhất theo phong cách kính mờ **Obsidian Dark & Crisp White**.
* Nền kính mờ sâu mang lại vẻ đẹp sang trọng, không che khuất góc nhìn khi đang thi đấu.

### 3. ⚡ Tối ưu hiệu năng & FPS
* Tối ưu hóa đường ống dẫn lệnh OpenGL, giảm độ trễ phím chuột và giữ ổn định khung hình trong các pha giao tranh nhiều hạt hiệu ứng.

### 4. 👤 Đổi tài khoản linh hoạt
* Đổi tên nhân vật trực tiếp khi đang ở trong trận hoặc sảnh chờ với nút **"Đổi & Vào Lại"** mà không cần khởi động lại game.

### 5. 💡 Fullbright (Phím B)
* Bấm phím **`B`** để tăng sáng tối đa trong hang tối mà không cần cắm đuốc.

---

## 🚀 Hướng dẫn khởi chạy

### Cách 1: Khởi chạy bằng Launcher Windows (Khuyên dùng)
1. Nhấp đúp vào **[`CasxDuoisLauncher.exe`](file:///d:/CasxDuois/CasxDuoisLauncher.exe)** (hoặc chạy **[`run_launcher.bat`](file:///d:/CasxDuois/run_launcher.bat)**).
2. **Không cần cài đặt thủ công Java:**
   * Launcher tự động nhận diện Java 8 có sẵn từ TLauncher, Minecraft Launcher chính thức, Lunar Client, CurseForge...
   * Nếu máy mới hoàn toàn chưa từng có Java, Launcher sẽ hiển thị nút **`⬇ Tải Java 8 Tự Động (~38MB)`** hoặc tự động hỏi bạn có muốn tải Java 8 Portable (Adoptium OpenJDK) khi bấm "Chơi ngay". Chỉ 1 cú nhấp chuột, tải và giải nén tự động 100%!
3. Chọn phiên bản:
   * **1.12.2 Forge & Mixin (Mới):** Đầy đủ Mod, Motion Blur, ClickGUI, HUD Editor.
   * **1.12.2 OptiFine (Lite):** Bản siêu nhẹ cho máy cấu hình văn phòng / AFK.
4. Điều chỉnh bộ nhớ RAM (mặc định 1024 MB).
5. Bấm **Chơi ngay** để vào game.

### Cách 2: Chơi trực tiếp trong TLauncher
1. Mở **[`CasxDuoisLauncher.exe`](file:///d:/CasxDuois/CasxDuoisLauncher.exe)** một lần để ứng dụng tự động đồng bộ mod `CasxDuoisMCClient-1.0.0.jar` vào thư mục `.minecraft/mods/`.
2. Mở TLauncher, chọn phiên bản **Forge 1.12.2** (hoặc **ForgeOptiFine 1.12.2**).
3. Bấm **Chơi** (Vào game) trong TLauncher, client sẽ tự động nạp cùng đầy đủ tính năng.

### Cách 3: Gói Portable 1-Click độc lập (Máy mới hoàn toàn)
* Chạy file **`CasxDuoisClient.exe`**: Tự động giải nén môi trường và tự động tải Java 8 Portable (~38 MB) trực tiếp nếu máy chưa có bất kỳ bản Java nào. Không cần quyền Admin, không cần thao tác kỹ thuật!

---

## 🛠️ Biên dịch từ mã nguồn

### Yêu cầu môi trường:
* **Java:** JDK 8 (Khuyến nghị Eclipse Adoptium Temurin 8 HotSpot).
* **.NET:** .NET Framework 4.5+ (Đã tích hợp sẵn trên Windows 10/11).

### Lệnh biên dịch:
```bash
# Biên dịch Mod Client JAR
./gradlew build

# Biên dịch Native Launcher C# (.exe)
& "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /target:winexe /out:CasxDuoisLauncher.exe /lib:"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\WPF" /r:PresentationFramework.dll /r:PresentationCore.dll /r:WindowsBase.dll /r:System.Xaml.dll /r:System.dll /r:System.Core.dll /win32icon:app.ico "src\launcher_native\CasxDuoisLauncherApp.cs"
```

---

## 📜 Bản quyền & Giấy phép
Dự án được xây dựng và phát hành phục vụ cộng đồng Minecraft. Vui lòng tôn trọng quyền tác giả khi chia sẻ hoặc phát triển tiếp.
