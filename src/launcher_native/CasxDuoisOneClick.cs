using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Reflection;
using System.Threading;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Effects;
using System.Windows.Shapes;

namespace CasxDuoisOneClick
{
    public class App : Application
    {
        [STAThread]
        public static void Main(string[] args)
        {
            App app = new App();
            app.Run(new MainWindow(args));
        }
    }

    public class MainWindow : Window
    {
        private ProgressBar prgBar;
        private TextBlock lblStatus;
        private TextBlock lblPercent;
        private readonly string[] launchArgs;

        public MainWindow(string[] args)
        {
            this.launchArgs = args;

            this.Title = "CasxDuois Client - 1-Click Portable";
            this.Width = 540;
            this.Height = 290;
            this.WindowStartupLocation = WindowStartupLocation.CenterScreen;
            this.WindowStyle = WindowStyle.None;
            this.ResizeMode = ResizeMode.NoResize;
            this.Background = new SolidColorBrush(Color.FromRgb(8, 9, 13)); // #08090D Obsidian Dark
            this.FontFamily = new FontFamily("Segoe UI, Inter, Arial");

            BuildUI();
            this.Loaded += (s, e) => { StartSetupProcess(); };
        }

        private void BuildUI()
        {
            Grid root = new Grid();
            root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(42) }); // Header
            root.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) }); // Content

            // ─── HEADER / TITLE BAR ───
            Border header = new Border
            {
                Background = new SolidColorBrush(Color.FromArgb(140, 11, 13, 20)),
                BorderBrush = new SolidColorBrush(Color.FromArgb(30, 255, 255, 255)),
                BorderThickness = new Thickness(0, 0, 0, 1)
            };
            header.MouseLeftButtonDown += (s, e) => { if (e.ButtonState == MouseButtonState.Pressed) DragMove(); };

            Grid headerGrid = new Grid { Margin = new Thickness(16, 0, 12, 0) };
            headerGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            headerGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(60) });

            StackPanel titlePanel = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };

            // Pill tag
            Border tag = new Border
            {
                Background = new SolidColorBrush(Color.FromArgb(35, 255, 255, 255)),
                BorderBrush = new SolidColorBrush(Color.FromArgb(60, 255, 255, 255)),
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(4),
                Padding = new Thickness(6, 2, 6, 2),
                Margin = new Thickness(0, 0, 10, 0)
            };
            tag.Child = new TextBlock
            {
                Text = "PORTABLE",
                FontSize = 9,
                FontWeight = FontWeights.Bold,
                Foreground = new SolidColorBrush(Colors.White)
            };

            TextBlock title = new TextBlock
            {
                Text = "CasxDuois Client · 1-Click Launcher",
                FontSize = 13,
                FontWeight = FontWeights.SemiBold,
                Foreground = new SolidColorBrush(Color.FromRgb(241, 245, 249)),
                VerticalAlignment = VerticalAlignment.Center
            };

            titlePanel.Children.Add(tag);
            titlePanel.Children.Add(title);
            Grid.SetColumn(titlePanel, 0);

            // Close / Minimize
            StackPanel winBtns = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
            Button btnMin = new Button
            {
                Content = "—",
                Width = 28,
                Height = 26,
                Background = Brushes.Transparent,
                Foreground = new SolidColorBrush(Color.FromRgb(148, 163, 184)),
                BorderThickness = new Thickness(0),
                Cursor = Cursors.Hand
            };
            btnMin.Click += (s, e) => { this.WindowState = WindowState.Minimized; };

            Button btnClose = new Button
            {
                Content = "✕",
                Width = 28,
                Height = 26,
                Background = Brushes.Transparent,
                Foreground = new SolidColorBrush(Color.FromRgb(148, 163, 184)),
                BorderThickness = new Thickness(0),
                Cursor = Cursors.Hand
            };
            btnClose.Click += (s, e) => { Application.Current.Shutdown(); };

            winBtns.Children.Add(btnMin);
            winBtns.Children.Add(btnClose);
            Grid.SetColumn(winBtns, 1);

            headerGrid.Children.Add(titlePanel);
            headerGrid.Children.Add(winBtns);
            header.Child = headerGrid;
            Grid.SetRow(header, 0);
            root.Children.Add(header);

            // ─── BODY CARD ───
            Border bodyCard = new Border
            {
                Margin = new Thickness(20),
                Background = new SolidColorBrush(Color.FromArgb(220, 14, 16, 24)),
                BorderBrush = new SolidColorBrush(Color.FromArgb(40, 255, 255, 255)),
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(10),
                Padding = new Thickness(22, 18, 22, 18),
                Effect = new DropShadowEffect { Color = Colors.Black, BlurRadius = 18, ShadowDepth = 4, Opacity = 0.55 }
            };

            StackPanel cardStack = new StackPanel();

            TextBlock mainHeading = new TextBlock
            {
                Text = "Tự động chuẩn bị & Khởi chạy Client",
                FontSize = 15,
                FontWeight = FontWeights.Bold,
                Foreground = new SolidColorBrush(Colors.White),
                Margin = new Thickness(0, 0, 0, 4)
            };

            TextBlock subHeading = new TextBlock
            {
                Text = "Tích hợp sẵn Portable Java 8, Forge 1.12.2, OptiFine và Client Mod độc lập.",
                FontSize = 11,
                Foreground = new SolidColorBrush(Color.FromRgb(148, 163, 184)),
                Margin = new Thickness(0, 0, 0, 22)
            };

            cardStack.Children.Add(mainHeading);
            cardStack.Children.Add(subHeading);

            // Progress Bar Container
            Border prgBorder = new Border
            {
                Height = 10,
                Background = new SolidColorBrush(Color.FromRgb(20, 24, 36)),
                BorderBrush = new SolidColorBrush(Color.FromArgb(30, 255, 255, 255)),
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(5),
                ClipToBounds = true,
                Margin = new Thickness(0, 0, 0, 10)
            };

            prgBar = new ProgressBar
            {
                Height = 10,
                Minimum = 0,
                Maximum = 100,
                Value = 0,
                Background = Brushes.Transparent,
                Foreground = new SolidColorBrush(Color.FromRgb(255, 255, 255)),
                BorderThickness = new Thickness(0)
            };
            prgBorder.Child = prgBar;
            cardStack.Children.Add(prgBorder);

            // Status Row
            Grid statusGrid = new Grid();
            statusGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            statusGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(80) });

            lblStatus = new TextBlock
            {
                Text = "Đang kiểm tra môi trường hệ thống...",
                FontSize = 11,
                Foreground = new SolidColorBrush(Color.FromRgb(203, 213, 225))
            };
            Grid.SetColumn(lblStatus, 0);

            lblPercent = new TextBlock
            {
                Text = "0%",
                FontSize = 11,
                FontWeight = FontWeights.Bold,
                Foreground = new SolidColorBrush(Colors.White),
                HorizontalAlignment = HorizontalAlignment.Right
            };
            Grid.SetColumn(lblPercent, 1);

            statusGrid.Children.Add(lblStatus);
            statusGrid.Children.Add(lblPercent);
            cardStack.Children.Add(statusGrid);

            bodyCard.Child = cardStack;
            Grid.SetRow(bodyCard, 1);
            root.Children.Add(bodyCard);

            this.Content = root;
        }

        private void StartSetupProcess()
        {
            new Thread(delegate()
            {
                try
                {
                    string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
                    string mcDir = System.IO.Path.Combine(appData, ".minecraft");
                    string modsDir = System.IO.Path.Combine(mcDir, "mods");
                    string jreExe = System.IO.Path.Combine(mcDir, "runtime", "jre-x64", "bin", "javaw.exe");
                    string modJar = System.IO.Path.Combine(modsDir, "CasxDuoisMCClient-1.0.0.jar");
                    string targetLauncher = System.IO.Path.Combine(mcDir, "CasxDuoisLauncher.exe");

                    string baseAppDir = AppDomain.CurrentDomain.BaseDirectory;
                    string localLauncher = System.IO.Path.Combine(baseAppDir, "CasxDuoisLauncher.exe");
                    string vJar = System.IO.Path.Combine(mcDir, "versions", "1.12.2", "1.12.2.jar");
                    string forgeJson = System.IO.Path.Combine(mcDir, "versions", "1.12.2-forge-14.23.5.2860", "1.12.2-forge-14.23.5.2860.json");

                    string cfgDir = System.IO.Path.Combine(appData, ".casxduois_launcher");
                    if (!Directory.Exists(cfgDir)) Directory.CreateDirectory(cfgDir);
                    string versionFile = System.IO.Path.Combine(cfgDir, "installed_app.version");

                    bool force = (launchArgs != null && (Array.IndexOf(launchArgs, "--force") >= 0 || Array.IndexOf(launchArgs, "--update") >= 0));

                    // Nhận diện dấu vết phiên bản file thực thi hiện tại
                    string curExePath = "";
                    try
                    {
                        curExePath = Assembly.GetExecutingAssembly().Location;
                        if (string.IsNullOrEmpty(curExePath) || !File.Exists(curExePath))
                        {
                            curExePath = Process.GetCurrentProcess().MainModule.FileName;
                        }
                    }
                    catch { }

                    string curVersionStamp = "";
                    if (!string.IsNullOrEmpty(curExePath) && File.Exists(curExePath))
                    {
                        FileInfo fi = new FileInfo(curExePath);
                        curVersionStamp = string.Format("{0}_{1}", fi.LastWriteTimeUtc.Ticks, fi.Length);
                    }

                    string installedVersionStamp = "";
                    if (File.Exists(versionFile))
                    {
                        try { installedVersionStamp = File.ReadAllText(versionFile).Trim(); } catch { }
                    }

                    bool isNewAppVersion = false;
                    if (force || string.IsNullOrEmpty(installedVersionStamp) || !string.Equals(installedVersionStamp, curVersionStamp, StringComparison.OrdinalIgnoreCase))
                    {
                        isNewAppVersion = true;
                    }

                    string tlJre = System.IO.Path.Combine(appData, ".tlauncher", "legacy", "Minecraft", "jre", "jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe");
                    bool hasJava = File.Exists(jreExe) || File.Exists(tlJre);

                    // Nếu không phải bản app mới và dữ liệu đã cài đặt đầy đủ thì mở ngay
                    if (!isNewAppVersion && hasJava && File.Exists(modJar) && File.Exists(vJar) && File.Exists(forgeJson) && (File.Exists(targetLauncher) || File.Exists(localLauncher)))
                    {
                        DeleteOldMixinJars(mcDir, modJar);
                        UpdateProgress(100, "✔ Dữ liệu đã sẵn sàng, đang mở CasxDuois Launcher...");
                        Thread.Sleep(500);
                        LaunchClientAndExit(File.Exists(localLauncher) ? localLauncher : targetLauncher);
                        return;
                    }

                    // ĐÃ PHÁT HIỆN BẢN APP MỚI: Xóa triệt để mixin bản cũ trước khi giải nén & khởi động
                    UpdateProgress(5, "Đã phát hiện bản app mới: Đang xóa mixin bản cũ...");
                    DeleteOldMixinJars(mcDir, null);
                    Thread.Sleep(200);

                    UpdateProgress(10, "Đang mở gói tài nguyên tích hợp...");
                    Assembly asm = Assembly.GetExecutingAssembly();
                    using (Stream resStream = asm.GetManifestResourceStream("ClientPayload"))
                    {
                        if (resStream == null)
                        {
                            UpdateProgress(0, "✖ Lỗi: Không tìm thấy gói tài nguyên tích hợp trong file exe!");
                            return;
                        }

                        using (ZipArchive archive = new ZipArchive(resStream, ZipArchiveMode.Read))
                        {
                            int totalEntries = archive.Entries.Count;
                            int current = 0;

                            foreach (ZipArchiveEntry entry in archive.Entries)
                            {
                                current++;
                                string normName = entry.FullName.Replace('/', System.IO.Path.DirectorySeparatorChar);
                                string destPath;
                                if (entry.FullName.Equals("CasxDuoisLauncher.exe", StringComparison.OrdinalIgnoreCase))
                                {
                                    destPath = targetLauncher;
                                    try
                                    {
                                        if (!localLauncher.Equals(targetLauncher, StringComparison.OrdinalIgnoreCase))
                                        {
                                            entry.ExtractToFile(localLauncher, true);
                                        }
                                    }
                                    catch { }
                                }
                                else
                                {
                                    destPath = System.IO.Path.Combine(appData, normName);
                                }

                                if (string.IsNullOrEmpty(entry.Name))
                                {
                                    if (!Directory.Exists(destPath)) Directory.CreateDirectory(destPath);
                                }
                                else
                                {
                                    string pDir = System.IO.Path.GetDirectoryName(destPath);
                                    if (!Directory.Exists(pDir)) Directory.CreateDirectory(pDir);

                                    try
                                    {
                                        entry.ExtractToFile(destPath, true);
                                    }
                                    catch { }
                                }

                                int percent = 5 + (int)((double)current / totalEntries * 90.0);
                                if (current % 15 == 0 || current == totalEntries)
                                {
                                    string fname = entry.Name;
                                    if (fname.Length > 30) fname = fname.Substring(0, 27) + "...";
                                    UpdateProgress(percent, string.Format("Đang giải nén: {0}", fname));
                                }
                            }
                        }
                    }

                    // Tự động tải Java 8 Portable nếu máy chưa có Java
                    string targetJreDir = System.IO.Path.Combine(mcDir, "runtime", "jre-x64");
                    string portableJavaw = System.IO.Path.Combine(targetJreDir, "bin", "javaw.exe");
                    bool javaReady = File.Exists(portableJavaw) || File.Exists(tlJre);

                    if (!javaReady)
                    {
                        try
                        {
                            UpdateProgress(90, "Đang tự động tải Java 8 Portable (~38 MB)...");
                            string tempJreZip = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "CasxDuois_Adoptium_Java8.zip");
                            ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072 | SecurityProtocolType.Tls;

                            using (WebClient wc = new WebClient())
                            {
                                wc.Headers.Add("User-Agent", "CasxDuoisInstaller/1.0");
                                AutoResetEvent dlDone = new AutoResetEvent(false);
                                wc.DownloadFileCompleted += (s, e) => dlDone.Set();
                                wc.DownloadProgressChanged += (s, e) =>
                                {
                                    int dlPercent = 90 + (int)(e.ProgressPercentage * 0.08);
                                    UpdateProgress(dlPercent, string.Format("Đang tải Java 8 Portable: {0}% ({1:F1}MB)", e.ProgressPercentage, e.BytesReceived / 1048576.0));
                                };
                                wc.DownloadFileAsync(new Uri("https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse"), tempJreZip);
                                dlDone.WaitOne();
                            }

                            UpdateProgress(98, "Đang giải nén Java 8 Portable...");
                            if (!Directory.Exists(targetJreDir)) Directory.CreateDirectory(targetJreDir);

                            using (ZipArchive jreArchive = ZipFile.OpenRead(tempJreZip))
                            {
                                string rootPrefix = "";
                                foreach (ZipArchiveEntry ent in jreArchive.Entries)
                                {
                                    string fn = ent.FullName.Replace('\\', '/');
                                    int bIdx = fn.IndexOf("/bin/", StringComparison.OrdinalIgnoreCase);
                                    if (bIdx >= 0)
                                    {
                                        rootPrefix = fn.Substring(0, bIdx + 1);
                                        break;
                                    }
                                }

                                foreach (ZipArchiveEntry entry in jreArchive.Entries)
                                {
                                    string rel = entry.FullName.Replace('\\', '/');
                                    if (!string.IsNullOrEmpty(rootPrefix) && rel.StartsWith(rootPrefix, StringComparison.OrdinalIgnoreCase))
                                    {
                                        rel = rel.Substring(rootPrefix.Length);
                                    }
                                    if (string.IsNullOrEmpty(rel)) continue;

                                    string dest = System.IO.Path.Combine(targetJreDir, rel.Replace('/', System.IO.Path.DirectorySeparatorChar));
                                    if (entry.FullName.EndsWith("/") || entry.FullName.EndsWith("\\"))
                                    {
                                        if (!Directory.Exists(dest)) Directory.CreateDirectory(dest);
                                    }
                                    else
                                    {
                                        string pDir = System.IO.Path.GetDirectoryName(dest);
                                        if (!Directory.Exists(pDir)) Directory.CreateDirectory(pDir);
                                        entry.ExtractToFile(dest, true);
                                    }
                                }
                            }

                            try { File.Delete(tempJreZip); } catch { }
                        }
                        catch { }
                    }

                    try
                    {
                        if (!string.IsNullOrEmpty(curVersionStamp))
                        {
                            File.WriteAllText(versionFile, curVersionStamp);
                        }
                    }
                    catch { }

                    // Tạo phím tắt Desktop tiện lợi
                    CreateDesktopShortcut(targetLauncher);

                    UpdateProgress(100, "✔ Cài đặt hoàn tất! Đang khởi chạy CasxDuois Launcher...");
                    Thread.Sleep(800);
                    LaunchClientAndExit(targetLauncher);
                }
                catch (Exception ex)
                {
                    UpdateProgress(0, "✖ Lỗi trong quá trình giải nén: " + ex.Message);
                }
            }).Start();
        }

        private static void DeleteOldMixinJars(string mcDir, string keepTarget)
        {
            try
            {
                string modsDir = System.IO.Path.Combine(mcDir, "mods");
                if (Directory.Exists(modsDir))
                {
                    string keepNorm = string.IsNullOrEmpty(keepTarget) ? "" : System.IO.Path.GetFullPath(keepTarget).ToLowerInvariant();
                    foreach (string f in Directory.GetFiles(modsDir, "*.jar"))
                    {
                        try
                        {
                            string fNorm = System.IO.Path.GetFullPath(f).ToLowerInvariant();
                            if (!string.IsNullOrEmpty(keepNorm) && fNorm == keepNorm)
                            {
                                continue;
                            }
                            string fn = System.IO.Path.GetFileName(f).ToLowerInvariant();
                            // Nhận diện và xóa mixin bản cũ: CasxDuois, Ascend, hoặc các mod mixin client khác
                            if (fn.Contains("casxduois") || fn.Contains("ascend") || (fn.Contains("mixin") && !fn.Contains("mixinbooter")))
                            {
                                try { File.SetAttributes(f, FileAttributes.Normal); } catch { }
                                File.Delete(f);
                            }
                        }
                        catch { }
                    }
                }

                // Xóa thư mục cache .mixin.out nếu tồn tại
                string mixinOut = System.IO.Path.Combine(mcDir, ".mixin.out");
                if (Directory.Exists(mixinOut))
                {
                    try { Directory.Delete(mixinOut, true); } catch { }
                }
            }
            catch { }
        }

        private void UpdateProgress(int percent, string statusText)
        {
            Dispatcher.Invoke((Action)delegate()
            {
                prgBar.Value = percent;
                lblPercent.Text = string.Format("{0}%", percent);
                lblStatus.Text = statusText;
            });
        }

        private void CreateDesktopShortcut(string launcherPath)
        {
            try
            {
                string desktop = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
                string shortcutPath = System.IO.Path.Combine(desktop, "CasxDuois Client.lnk");
                Type shellType = Type.GetTypeFromProgID("WScript.Shell");
                if (shellType != null)
                {
                    dynamic shell = Activator.CreateInstance(shellType);
                    dynamic shortcut = shell.CreateShortcut(shortcutPath);
                    shortcut.TargetPath = launcherPath;
                    shortcut.WorkingDirectory = System.IO.Path.GetDirectoryName(launcherPath);
                    shortcut.IconLocation = launcherPath + ",0";
                    shortcut.Description = "CasxDuois Client - Minecraft 1.12.2";
                    shortcut.Save();
                }
            }
            catch { }
        }

        private void LaunchClientAndExit(string launcherPath)
        {
            try
            {
                if (File.Exists(launcherPath))
                {
                    ProcessStartInfo psi = new ProcessStartInfo(launcherPath);
                    psi.WorkingDirectory = System.IO.Path.GetDirectoryName(launcherPath);
                    Process.Start(psi);
                }
            }
            catch { }

            Dispatcher.Invoke((Action)delegate()
            {
                Application.Current.Shutdown();
            });
        }
    }
}
