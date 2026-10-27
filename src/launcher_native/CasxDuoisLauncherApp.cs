using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Effects;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace CasxDuoisLauncher
{
    public class App : Application
    {
        [STAThread]
        public static void Main()
        {
            App app = new App();
            app.Run(new MainWindow());
        }
    }

    public class MainWindow : Window
    {
        // ─── Win32 API để đổi Title cửa sổ Game thành CasxDuoisMC_<name> ───
        [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        static extern bool SetWindowText(IntPtr hWnd, string lpString);

        [DllImport("user32.dll")]
        static extern bool EnumThreadWindows(int dwThreadId, EnumThreadDelegate lpfn, IntPtr lParam);
        delegate bool EnumThreadDelegate(IntPtr hWnd, IntPtr lParam);

        // ─── Theme Colors: Monochrome Obsidian & Crisp White (Matching New Icon) ───
        static readonly Color ClrBG          = Color.FromRgb(8, 9, 13);         // #08090D Deep Obsidian
        static readonly Color ClrCard        = Color.FromArgb(238, 14, 16, 24); // #0E1018 Frosted Glass Card
        static readonly Color ClrInput       = Color.FromRgb(11, 13, 20);       // #0B0D14 Nền ô nhập
        static readonly Color ClrBorderCard  = Color.FromArgb(48, 255, 255, 255); // Viền kính mờ
        static readonly Color ClrBorderInput = Color.FromArgb(38, 255, 255, 255);
        static readonly Color ClrWhite       = Color.FromRgb(255, 255, 255);    // #FFFFFF Trắng tinh
        static readonly Color ClrSilver      = Color.FromRgb(203, 213, 225);    // #CBD5E1 Bạc ánh kim
        static readonly Color ClrTextWhite   = Color.FromRgb(255, 255, 255);
        static readonly Color ClrTextMuted   = Color.FromRgb(148, 163, 184);    // #94A3B8
        static readonly Color ClrTextDark    = Color.FromRgb(100, 116, 139);    // #64748B
        static readonly Color ClrCyan        = Color.FromRgb(56, 189, 248);     // #38BDF8 Chỉ báo Cyan
        static readonly Color ClrGreen       = Color.FromRgb(34, 197, 94);      // #22C55E Xanh trạng thái
        static readonly Color ClrRed         = Color.FromRgb(239, 68, 68);      // #EF4444 Đỏ cảnh báo

        // ─── State ───
        readonly List<string> accounts = new List<string>();
        readonly string configDir;
        readonly string accountsFile;
        readonly string configFile;
        int currentMode = 0; // 0: Mixin x Forge (Mới), 1: OptiFine Standalone (Cũ)

        // ─── UI Controls ───
        TextBox txtNick;
        Border txtNickBorder;
        ComboBox cmbMode;
        ComboBox cmbAccounts;
        Slider sldRam;
        TextBlock lblRamVal;
        CheckBox chkAutoClose;
        TextBlock lblStatus;
        TextBlock lblJavaInfo;
        Button btnDownloadJava;
        StackPanel spDownload;
        ProgressBar prgDownload;
        TextBlock lblDownloadStatus;
        bool isDownloadingJava = false;
        Button btnPlay;
        string detectedJavaExe = "";
        string detectedJavaDesc = "";

        public MainWindow()
        {
            this.Title = "Minecraft Client";
            this.Width = 840;
            this.Height = 650;
            this.MinWidth = 700;
            this.MinHeight = 560;
            this.WindowStartupLocation = WindowStartupLocation.CenterScreen;
            this.WindowStyle = WindowStyle.None;
            this.ResizeMode = ResizeMode.CanResizeWithGrip;
            this.Background = new SolidColorBrush(ClrBG);
            this.FontFamily = new FontFamily("Segoe UI, Inter, Arial");

            configDir = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".casxduois_launcher");
            if (!Directory.Exists(configDir)) Directory.CreateDirectory(configDir);
            accountsFile = System.IO.Path.Combine(configDir, "accounts.txt");
            configFile = System.IO.Path.Combine(configDir, "config.json");

            ApplyDarkStyles();
            LoadAccounts();
            BuildNativeUI();
            LoadConfig();
            ApplyModeChange(currentMode);
            SyncModAndProfiles();
            RefreshJavaDetection();
        }

        private void ApplyDarkStyles()
        {
            string xaml = @"
<ResourceDictionary xmlns=""http://schemas.microsoft.com/winfx/2006/xaml/presentation""
                    xmlns:x=""http://schemas.microsoft.com/winfx/2006/xaml"">
    <Style TargetType=""{x:Type ComboBoxItem}"">
        <Setter Property=""SnapsToDevicePixels"" Value=""True""/>
        <Setter Property=""OverridesDefaultStyle"" Value=""True""/>
        <Setter Property=""Foreground"" Value=""#FFFFFF""/>
        <Setter Property=""FontSize"" Value=""12""/>
        <Setter Property=""FontWeight"" Value=""SemiBold""/>
        <Setter Property=""Cursor"" Value=""Hand""/>
        <Setter Property=""Template"">
            <Setter.Value>
                <ControlTemplate TargetType=""{x:Type ComboBoxItem}"">
                    <Border Name=""ItemBorder""
                            Background=""Transparent""
                            BorderThickness=""0""
                            CornerRadius=""6""
                            Padding=""12,8,12,8""
                            Margin=""2,1,2,1"">
                        <ContentPresenter VerticalAlignment=""Center""/>
                    </Border>
                    <ControlTemplate.Triggers>
                        <Trigger Property=""IsHighlighted"" Value=""True"">
                            <Setter TargetName=""ItemBorder"" Property=""Background"" Value=""#25FFFFFF""/>
                        </Trigger>
                        <Trigger Property=""IsSelected"" Value=""True"">
                            <Setter TargetName=""ItemBorder"" Property=""Background"" Value=""#35FFFFFF""/>
                        </Trigger>
                    </ControlTemplate.Triggers>
                </ControlTemplate>
            </Setter.Value>
        </Setter>
    </Style>

    <Style TargetType=""{x:Type ComboBox}"">
        <Setter Property=""SnapsToDevicePixels"" Value=""True""/>
        <Setter Property=""OverridesDefaultStyle"" Value=""True""/>
        <Setter Property=""ScrollViewer.HorizontalScrollBarVisibility"" Value=""Auto""/>
        <Setter Property=""ScrollViewer.VerticalScrollBarVisibility"" Value=""Auto""/>
        <Setter Property=""ScrollViewer.CanContentScroll"" Value=""True""/>
        <Setter Property=""Foreground"" Value=""#FFFFFF""/>
        <Setter Property=""FontSize"" Value=""12""/>
        <Setter Property=""FontWeight"" Value=""SemiBold""/>
        <Setter Property=""Cursor"" Value=""Hand""/>
        <Setter Property=""Template"">
            <Setter.Value>
                <ControlTemplate TargetType=""{x:Type ComboBox}"">
                    <Grid>
                        <ToggleButton Name=""ToggleButton""
                                      Focusable=""False""
                                      ClickMode=""Press""
                                      IsChecked=""{Binding Path=IsDropDownOpen, Mode=TwoWay, RelativeSource={RelativeSource TemplatedParent}}"">
                            <ToggleButton.Template>
                                <ControlTemplate TargetType=""{x:Type ToggleButton}"">
                                    <Border Name=""Border""
                                            Background=""#0B0D14""
                                            BorderBrush=""#38FFFFFF""
                                            BorderThickness=""1""
                                            CornerRadius=""8""
                                            Padding=""12,0,10,0"">
                                        <Grid>
                                            <Grid.ColumnDefinitions>
                                                <ColumnDefinition Width=""*""/>
                                                <ColumnDefinition Width=""20""/>
                                            </Grid.ColumnDefinitions>
                                            <Path Grid.Column=""1""
                                                  Data=""M 0 0 L 4 4 L 8 0 Z""
                                                  Fill=""#CBD5E1""
                                                  HorizontalAlignment=""Center""
                                                  VerticalAlignment=""Center""/>
                                        </Grid>
                                    </Border>
                                    <ControlTemplate.Triggers>
                                        <Trigger Property=""IsMouseOver"" Value=""True"">
                                            <Setter TargetName=""Border"" Property=""BorderBrush"" Value=""#80FFFFFF""/>
                                            <Setter TargetName=""Border"" Property=""Background"" Value=""#141824""/>
                                        </Trigger>
                                    </ControlTemplate.Triggers>
                                </ControlTemplate>
                            </ToggleButton.Template>
                        </ToggleButton>
                        <ContentPresenter Name=""ContentSite""
                                          IsHitTestVisible=""False""
                                          Content=""{TemplateBinding SelectionBoxItem}""
                                          ContentTemplate=""{TemplateBinding SelectionBoxItemTemplate}""
                                          ContentTemplateSelector=""{TemplateBinding ItemTemplateSelector}""
                                          Margin=""12,0,28,0""
                                          VerticalAlignment=""Center""
                                          HorizontalAlignment=""Left""/>
                        <Popup Name=""Popup""
                               Placement=""Bottom""
                               IsOpen=""{TemplateBinding IsDropDownOpen}""
                               AllowsTransparency=""True""
                               Focusable=""False""
                               PopupAnimation=""Slide"">
                            <Grid Name=""DropDown""
                                  SnapsToDevicePixels=""True""
                                  MinWidth=""{TemplateBinding ActualWidth}""
                                  MaxHeight=""{TemplateBinding MaxDropDownHeight}"">
                                <Border Name=""DropDownBorder""
                                        Background=""#0E1018""
                                        BorderThickness=""1""
                                        BorderBrush=""#40FFFFFF""
                                        CornerRadius=""8""
                                        Margin=""0,4,0,4"">
                                    <ScrollViewer Margin=""4,6,4,6"" SnapsToDevicePixels=""True"">
                                        <StackPanel IsItemsHost=""True"" KeyboardNavigation.DirectionalNavigation=""Contained"" />
                                    </ScrollViewer>
                                </Border>
                            </Grid>
                        </Popup>
                    </Grid>
                </ControlTemplate>
            </Setter.Value>
        </Setter>
    </Style>

    <Style TargetType=""{x:Type Slider}"">
        <Setter Property=""SnapsToDevicePixels"" Value=""True""/>
        <Setter Property=""OverridesDefaultStyle"" Value=""True""/>
        <Setter Property=""Cursor"" Value=""Hand""/>
        <Setter Property=""Template"">
            <Setter.Value>
                <ControlTemplate TargetType=""{x:Type Slider}"">
                    <Grid VerticalAlignment=""Center"" Height=""24"">
                        <Border Height=""5"" CornerRadius=""2.5"" Background=""#22FFFFFF"" BorderThickness=""0"" VerticalAlignment=""Center""/>
                        <Track Name=""PART_Track"">
                            <Track.Thumb>
                                <Thumb Cursor=""Hand"">
                                    <Thumb.Template>
                                        <ControlTemplate TargetType=""{x:Type Thumb}"">
                                            <Ellipse Width=""16"" Height=""16"" Fill=""#FFFFFF"" Stroke=""#94A3B8"" StrokeThickness=""1"">
                                                <Ellipse.Effect>
                                                    <DropShadowEffect Color=""#FFFFFF"" BlurRadius=""8"" ShadowDepth=""0"" Opacity=""0.5""/>
                                                </Ellipse.Effect>
                                            </Ellipse>
                                        </ControlTemplate>
                                    </Thumb.Template>
                                </Thumb>
                            </Track.Thumb>
                        </Track>
                    </Grid>
                </ControlTemplate>
            </Setter.Value>
        </Setter>
    </Style>
</ResourceDictionary>";

            try
            {
                ResourceDictionary rd = (ResourceDictionary)System.Windows.Markup.XamlReader.Parse(xaml);
                this.Resources.MergedDictionaries.Add(rd);
            }
            catch { }
        }

        private void BuildNativeUI()
        {
            Grid rootGrid = new Grid();
            rootGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(40) });
            rootGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            rootGrid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(32) });

            // =================================================================
            // 1. TITLE BAR (Frosted Dark Glass)
            // =================================================================
            Border titleBar = new Border
            {
                Background = new SolidColorBrush(Color.FromRgb(10, 12, 17)),
                BorderBrush = new SolidColorBrush(Color.FromArgb(40, 255, 255, 255)),
                BorderThickness = new Thickness(0, 0, 0, 1),
                Padding = new Thickness(16, 0, 10, 0)
            };
            titleBar.MouseLeftButtonDown += (s, e) => { if (e.ButtonState == MouseButtonState.Pressed) this.DragMove(); };

            Grid titleGrid = new Grid();
            StackPanel spTitleLeft = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
            
            // Icon thumbnail in titlebar
            string iconThumbPath = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "app_ui", "logo.png");
            if (File.Exists(iconThumbPath))
            {
                Image iconImg = new Image
                {
                    Width = 18,
                    Height = 18,
                    Source = new BitmapImage(new Uri(iconThumbPath, UriKind.Absolute)),
                    Margin = new Thickness(0, 0, 9, 0),
                    VerticalAlignment = VerticalAlignment.Center
                };
                RenderOptions.SetBitmapScalingMode(iconImg, BitmapScalingMode.HighQuality);
                spTitleLeft.Children.Add(iconImg);
            }
            else
            {
                TextBlock iconBolt = new TextBlock { Text = "⚡", FontSize = 12, Foreground = new SolidColorBrush(ClrWhite), Margin = new Thickness(0, 0, 8, 0) };
                spTitleLeft.Children.Add(iconBolt);
            }

            TextBlock titleText = new TextBlock { Text = "Minecraft Client", FontSize = 12.5, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(ClrWhite), VerticalAlignment = VerticalAlignment.Center };
            spTitleLeft.Children.Add(titleText);

            StackPanel spWinBtns = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Right };
            Button btnMin = CreateWinButton("─", ClrTextMuted, () => { this.WindowState = WindowState.Minimized; });
            Button btnMax = CreateWinButton("□", ClrTextMuted, () => {
                this.WindowState = (this.WindowState == WindowState.Maximized) ? WindowState.Normal : WindowState.Maximized;
            });
            Button btnClose = CreateWinButton("✕", ClrRed, () => { Application.Current.Shutdown(); });

            spWinBtns.Children.Add(btnMin);
            spWinBtns.Children.Add(btnMax);
            spWinBtns.Children.Add(btnClose);

            titleGrid.Children.Add(spTitleLeft);
            titleGrid.Children.Add(spWinBtns);
            titleBar.Child = titleGrid;
            Grid.SetRow(titleBar, 0);
            rootGrid.Children.Add(titleBar);

            // =================================================================
            // 2. STATUS BAR
            // =================================================================
            Border statusBar = new Border
            {
                Background = new SolidColorBrush(Color.FromRgb(9, 10, 14)),
                BorderBrush = new SolidColorBrush(Color.FromArgb(35, 255, 255, 255)),
                BorderThickness = new Thickness(0, 1, 0, 0),
                Padding = new Thickness(16, 0, 16, 0)
            };
            Grid statusGrid = new Grid();

            lblStatus = new TextBlock { Visibility = Visibility.Collapsed };

            StackPanel spJavaStatus = new StackPanel
            {
                Orientation = Orientation.Horizontal,
                VerticalAlignment = VerticalAlignment.Center,
                HorizontalAlignment = HorizontalAlignment.Left
            };

            lblJavaInfo = new TextBlock
            {
                Text = "☕ Đang kiểm tra Java 8...",
                FontSize = 10.5,
                FontWeight = FontWeights.SemiBold,
                Foreground = new SolidColorBrush(ClrSilver),
                VerticalAlignment = VerticalAlignment.Center
            };

            btnDownloadJava = new Button
            {
                Content = "⬇ Tải Java 8 Tự Động (~38MB)",
                FontSize = 10,
                FontWeight = FontWeights.Bold,
                Height = 22,
                Padding = new Thickness(8, 0, 8, 0),
                Margin = new Thickness(10, 0, 0, 0),
                Background = new SolidColorBrush(Color.FromRgb(30, 41, 59)),
                Foreground = new SolidColorBrush(ClrCyan),
                BorderBrush = new SolidColorBrush(Color.FromArgb(80, 56, 189, 248)),
                BorderThickness = new Thickness(1),
                Cursor = Cursors.Hand,
                Visibility = Visibility.Collapsed
            };
            btnDownloadJava.Click += (s, e) => { StartDownloadJava8(); };

            spJavaStatus.Children.Add(lblJavaInfo);
            spJavaStatus.Children.Add(btnDownloadJava);

            TextBlock lblVerStatus = new TextBlock
            {
                Text = "CasxDuois v1.0.0",
                FontSize = 10,
                FontWeight = FontWeights.Normal,
                Foreground = new SolidColorBrush(ClrTextDark),
                HorizontalAlignment = HorizontalAlignment.Right,
                VerticalAlignment = VerticalAlignment.Center
            };

            statusGrid.Children.Add(spJavaStatus);
            statusGrid.Children.Add(lblVerStatus);
            statusBar.Child = statusGrid;
            Grid.SetRow(statusBar, 2);
            rootGrid.Children.Add(statusBar);

            // =================================================================
            // 3. MAIN BODY (Centered Card)
            // =================================================================
            ScrollViewer scrollBody = new ScrollViewer
            {
                VerticalScrollBarVisibility = ScrollBarVisibility.Auto,
                HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled,
                Padding = new Thickness(20, 16, 20, 16)
            };

            StackPanel centerPanel = new StackPanel
            {
                Width = 580,
                HorizontalAlignment = HorizontalAlignment.Center,
                VerticalAlignment = VerticalAlignment.Center
            };

            // --- 3.1 HERO LOGO & TITLE ---
            StackPanel spHero = new StackPanel { HorizontalAlignment = HorizontalAlignment.Center, Margin = new Thickness(0, 0, 0, 16) };

            string logoPath = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "app_ui", "logo.png");
            if (!File.Exists(logoPath)) logoPath = @"d:\CasxDuois\app_ui\logo.png";

            if (File.Exists(logoPath))
            {
                Image logoImg = new Image
                {
                    Width = 84,
                    Height = 84,
                    Source = new BitmapImage(new Uri(logoPath, UriKind.Absolute)),
                    Effect = new DropShadowEffect { Color = Colors.White, BlurRadius = 24, ShadowDepth = 0, Opacity = 0.45 }
                };
                RenderOptions.SetBitmapScalingMode(logoImg, BitmapScalingMode.HighQuality);
                spHero.Children.Add(logoImg);
            }

            TextBlock txtTitle = new TextBlock
            {
                Text = "CasxDuois",
                FontSize = 22,
                FontWeight = FontWeights.Bold,
                Foreground = new SolidColorBrush(ClrWhite),
                HorizontalAlignment = HorizontalAlignment.Center,
                Margin = new Thickness(0, 8, 0, 0)
            };
            spHero.Children.Add(txtTitle);
            centerPanel.Children.Add(spHero);

            // --- 3.2 MAIN CARD ---
            Border mainCard = new Border
            {
                Background = new SolidColorBrush(ClrCard),
                BorderBrush = new SolidColorBrush(ClrBorderCard),
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(16),
                Padding = new Thickness(24, 22, 24, 22),
                Effect = new DropShadowEffect { Color = Colors.Black, BlurRadius = 40, ShadowDepth = 14, Opacity = 0.75 }
            };

            StackPanel cardStack = new StackPanel();

            // =================================================================
            // [HÀNG 1] QUẢN LÝ TÀI KHOẢN (2 CỘT CÂN ĐỐI)
            // =================================================================
            Grid gridAccRow = new Grid { Margin = new Thickness(0, 0, 0, 14) };
            gridAccRow.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            gridAccRow.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(16) });
            gridAccRow.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            // Cột trái: Tài khoản đã lưu (Chọn & Xóa)
            StackPanel spAccLeft = new StackPanel();
            TextBlock lblAccSelect = new TextBlock { Text = "Tài khoản", FontSize = 11, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(ClrSilver), Margin = new Thickness(0, 0, 0, 6) };
            spAccLeft.Children.Add(lblAccSelect);

            Grid gridAccSelect = new Grid();
            gridAccSelect.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            gridAccSelect.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(6) });
            gridAccSelect.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(64) });

            cmbAccounts = new ComboBox
            {
                Height = 36,
                FontSize = 12,
                FontWeight = FontWeights.SemiBold
            };
            cmbAccounts.SelectionChanged += (s, e) =>
            {
                if (cmbAccounts.SelectedIndex >= 0 && cmbAccounts.SelectedIndex < accounts.Count)
                {
                    txtNick.Text = accounts[cmbAccounts.SelectedIndex];
                }
            };
            Grid.SetColumn(cmbAccounts, 0);

            Button btnDelAcc = CreateButton("✕ Xóa", ClrRed, Color.FromArgb(30, 239, 68, 68), () => DoDeleteAccount());
            btnDelAcc.Height = 36;
            btnDelAcc.FontSize = 11;
            Grid.SetColumn(btnDelAcc, 2);

            gridAccSelect.Children.Add(cmbAccounts);
            gridAccSelect.Children.Add(btnDelAcc);
            spAccLeft.Children.Add(gridAccSelect);
            Grid.SetColumn(spAccLeft, 0);

            // Cột phải: Thêm tài khoản mới
            StackPanel spAccRight = new StackPanel();
            TextBlock lblAccAdd = new TextBlock { Text = "Thêm tài khoản", FontSize = 11, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(ClrSilver), Margin = new Thickness(0, 0, 0, 6) };
            spAccRight.Children.Add(lblAccAdd);

            Grid gridAccAdd = new Grid();
            gridAccAdd.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            gridAccAdd.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(6) });
            gridAccAdd.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(72) });

            txtNickBorder = new Border
            {
                Height = 36,
                Background = new SolidColorBrush(ClrInput),
                BorderBrush = new SolidColorBrush(ClrBorderInput),
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(8),
                Padding = new Thickness(12, 0, 12, 0)
            };

            txtNick = new TextBox
            {
                Background = Brushes.Transparent,
                Foreground = new SolidColorBrush(ClrTextWhite),
                CaretBrush = new SolidColorBrush(ClrWhite),
                BorderThickness = new Thickness(0),
                FontSize = 12,
                FontWeight = FontWeights.SemiBold,
                VerticalContentAlignment = VerticalAlignment.Center,
                Text = accounts.Count > 0 ? accounts[0] : "CasxDuoisPlayer"
            };
            txtNick.GotFocus += (s, e) => { txtNickBorder.BorderBrush = new SolidColorBrush(ClrWhite); };
            txtNick.LostFocus += (s, e) => { txtNickBorder.BorderBrush = new SolidColorBrush(ClrBorderInput); };
            txtNick.KeyDown += (s, e) => { if (e.Key == Key.Enter) DoAddAccount(); };
            txtNickBorder.Child = txtNick;
            Grid.SetColumn(txtNickBorder, 0);

            Button btnAddNick = CreateButton("➕ Thêm", ClrWhite, Color.FromArgb(25, 255, 255, 255), () => DoAddAccount());
            btnAddNick.Height = 36;
            btnAddNick.FontSize = 11;
            Grid.SetColumn(btnAddNick, 2);

            gridAccAdd.Children.Add(txtNickBorder);
            gridAccAdd.Children.Add(btnAddNick);
            spAccRight.Children.Add(gridAccAdd);
            Grid.SetColumn(spAccRight, 2);

            gridAccRow.Children.Add(spAccLeft);
            gridAccRow.Children.Add(spAccRight);
            cardStack.Children.Add(gridAccRow);

            // =================================================================
            // [HÀNG 2] CÀI ĐẶT GAME: PHIÊN BẢN & BỘ NHỚ RAM (2 CỘT CÂN ĐỐI)
            // =================================================================
            Grid gridSettings = new Grid { Margin = new Thickness(0, 0, 0, 14) };
            gridSettings.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            gridSettings.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(16) });
            gridSettings.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            // Cột trái: Phiên bản
            StackPanel spVer = new StackPanel();
            TextBlock lblVerHdr = new TextBlock { Text = "Phiên bản", FontSize = 11, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(ClrSilver), Margin = new Thickness(0, 0, 0, 6) };
            spVer.Children.Add(lblVerHdr);

            cmbMode = new ComboBox
            {
                Height = 36,
                FontSize = 12,
                FontWeight = FontWeights.SemiBold
            };
            cmbMode.Items.Add("Forge 1.12.2");
            cmbMode.Items.Add("Optifine 1.12.2");
            cmbMode.SelectedIndex = 0;
            cmbMode.SelectionChanged += (s, e) =>
            {
                if (cmbMode.SelectedIndex >= 0)
                {
                    ApplyModeChange(cmbMode.SelectedIndex);
                    SaveConfig();
                }
            };
            spVer.Children.Add(cmbMode);
            Grid.SetColumn(spVer, 0);

            // Cột phải: Bộ nhớ RAM (Bao gồm giá trị và Slider trong khung bo góc 36px)
            StackPanel spRam = new StackPanel();
            Grid gridRamLabel = new Grid { Margin = new Thickness(0, 0, 0, 6) };
            TextBlock lblRamHdr = new TextBlock { Text = "Bộ nhớ RAM", FontSize = 11, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(ClrSilver) };
            lblRamVal = new TextBlock { Text = "1024 MB", FontSize = 11, FontWeight = FontWeights.Bold, Foreground = new SolidColorBrush(ClrWhite), HorizontalAlignment = HorizontalAlignment.Right };
            gridRamLabel.Children.Add(lblRamHdr);
            gridRamLabel.Children.Add(lblRamVal);

            Border sldContainer = new Border
            {
                Height = 36,
                Background = new SolidColorBrush(ClrInput),
                BorderBrush = new SolidColorBrush(ClrBorderInput),
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(8),
                Padding = new Thickness(12, 0, 12, 0)
            };

            sldRam = new Slider
            {
                Minimum = 512,
                Maximum = 4096,
                Value = 1024,
                SmallChange = 128,
                LargeChange = 256,
                IsSnapToTickEnabled = true,
                TickFrequency = 256,
                Height = 24,
                VerticalAlignment = VerticalAlignment.Center
            };
            sldRam.ValueChanged += (s, e) =>
            {
                lblRamVal.Text = string.Format("{0} MB", (int)sldRam.Value);
                SaveConfig();
            };
            sldContainer.Child = sldRam;

            spRam.Children.Add(gridRamLabel);
            spRam.Children.Add(sldContainer);
            Grid.SetColumn(spRam, 2);

            gridSettings.Children.Add(spVer);
            gridSettings.Children.Add(spRam);
            cardStack.Children.Add(gridSettings);

            // =================================================================
            // [HÀNG 3] TÙY CHỌN & TIỆN ÍCH (TỰ ĐỘNG ĐÓNG & THƯ MỤC .MINECRAFT)
            // =================================================================
            Grid gridOptions = new Grid { Margin = new Thickness(0, 0, 0, 18) };
            gridOptions.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1.35, GridUnitType.Star) });
            gridOptions.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(16) });
            gridOptions.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

            chkAutoClose = new CheckBox
            {
                Content = "Tự động đóng Launcher sau khi mở game",
                IsChecked = true,
                Foreground = new SolidColorBrush(ClrSilver),
                FontSize = 11,
                FontWeight = FontWeights.SemiBold,
                VerticalAlignment = VerticalAlignment.Center
            };
            chkAutoClose.Checked += (s, e) => SaveConfig();
            chkAutoClose.Unchecked += (s, e) => SaveConfig();
            Grid.SetColumn(chkAutoClose, 0);

            Button btnFolder = CreateButton("📁 Mở thư mục .minecraft", ClrSilver, Color.FromArgb(16, 255, 255, 255), () =>
            {
                string mc = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
                Process.Start("explorer.exe", mc);
            });
            btnFolder.Height = 32;
            btnFolder.FontSize = 11;
            Grid.SetColumn(btnFolder, 2);

            gridOptions.Children.Add(chkAutoClose);
            gridOptions.Children.Add(btnFolder);
            cardStack.Children.Add(gridOptions);

            // =================================================================
            // [DOWNLOAD PROGRESS] TIẾN ĐỘ TẢI JAVA 8 PORTABLE TỰ ĐỘNG
            // =================================================================
            spDownload = new StackPanel
            {
                Margin = new Thickness(0, 0, 0, 14),
                Visibility = Visibility.Collapsed
            };

            Grid gridDlHeader = new Grid { Margin = new Thickness(0, 0, 0, 6) };
            lblDownloadStatus = new TextBlock
            {
                Text = "Đang tải Java 8 Portable (Adoptium OpenJDK 8)...",
                FontSize = 11,
                FontWeight = FontWeights.SemiBold,
                Foreground = new SolidColorBrush(ClrSilver)
            };
            gridDlHeader.Children.Add(lblDownloadStatus);

            Border prgBorder = new Border
            {
                Height = 10,
                Background = new SolidColorBrush(ClrInput),
                BorderBrush = new SolidColorBrush(ClrBorderInput),
                BorderThickness = new Thickness(1),
                CornerRadius = new CornerRadius(5),
                ClipToBounds = true
            };

            prgDownload = new ProgressBar
            {
                Height = 8,
                Minimum = 0,
                Maximum = 100,
                Value = 0,
                Foreground = new SolidColorBrush(ClrCyan),
                Background = Brushes.Transparent,
                BorderThickness = new Thickness(0)
            };
            prgBorder.Child = prgDownload;

            spDownload.Children.Add(gridDlHeader);
            spDownload.Children.Add(prgBorder);
            cardStack.Children.Add(spDownload);

            // =================================================================
            // [HÀNG 4] NÚT CHƠI NGAY - CALL TO ACTION TRANG TRỌNG Ở ĐÁY CARD
            // =================================================================
            btnPlay = new Button
            {
                Height = 46,
                Cursor = Cursors.Hand,
                Background = new SolidColorBrush(ClrWhite),
                Foreground = new SolidColorBrush(Color.FromRgb(8, 9, 13)),
                FontSize = 14,
                FontWeight = FontWeights.Bold,
                BorderThickness = new Thickness(0),
                Effect = new DropShadowEffect { Color = Colors.White, BlurRadius = 24, ShadowDepth = 0, Opacity = 0.45 }
            };
            btnPlay.Content = "▶   Chơi ngay";
            btnPlay.Click += (s, e) => { LaunchGame(); };
            btnPlay.MouseEnter += (s, e) =>
            {
                btnPlay.Effect = new DropShadowEffect { Color = Colors.White, BlurRadius = 32, ShadowDepth = 0, Opacity = 0.7 };
            };
            btnPlay.MouseLeave += (s, e) =>
            {
                btnPlay.Effect = new DropShadowEffect { Color = Colors.White, BlurRadius = 24, ShadowDepth = 0, Opacity = 0.45 };
            };
            cardStack.Children.Add(btnPlay);

            mainCard.Child = cardStack;
            centerPanel.Children.Add(mainCard);
            scrollBody.Content = centerPanel;
            Grid.SetRow(scrollBody, 1);
            rootGrid.Children.Add(scrollBody);

            this.Content = rootGrid;
            RefreshAccountsDropdown();
        }

        // ═════════════════════════════════════════════════════════════════
        //  MODE MANAGEMENT
        // ═════════════════════════════════════════════════════════════════
        private void ApplyModeChange(int modeIndex)
        {
            currentMode = modeIndex;
            if (cmbMode != null && cmbMode.SelectedIndex != modeIndex)
            {
                cmbMode.SelectedIndex = modeIndex;
            }

            if (btnPlay != null) btnPlay.Content = "▶   Chơi ngay";
        }

        // ═════════════════════════════════════════════════════════════════
        //  ACTIONS & LOGIC
        // ═════════════════════════════════════════════════════════════════
        private void RefreshAccountsDropdown()
        {
            if (cmbAccounts == null) return;
            cmbAccounts.Items.Clear();
            foreach (string acc in accounts)
            {
                cmbAccounts.Items.Add("👤 " + acc);
            }
            if (accounts.Count > 0)
            {
                string cur = txtNick != null ? txtNick.Text.Trim() : "";
                int idx = accounts.IndexOf(cur);
                if (idx >= 0)
                    cmbAccounts.SelectedIndex = idx;
                else
                {
                    cmbAccounts.SelectedIndex = 0;
                    if (txtNick != null) txtNick.Text = accounts[0];
                }
            }
        }

        private void DoAddAccount()
        {
            string n = txtNick.Text.Trim();
            if (!string.IsNullOrEmpty(n))
            {
                if (!accounts.Contains(n))
                {
                    accounts.Add(n);
                    SaveAccounts();
                }
                RefreshAccountsDropdown();
                int idx = accounts.IndexOf(n);
                if (idx >= 0) cmbAccounts.SelectedIndex = idx;
                SetStatus("Đã chọn/thêm tài khoản: " + n, ClrGreen);
            }
        }

        private void DoAddBots()
        {
            int seed = (DateTime.Now.Millisecond % 90) + 10;
            for (int i = 1; i <= 5; i++)
            {
                string bot = string.Format("BotAFK_{0}_{1:D2}", seed, i);
                if (!accounts.Contains(bot)) accounts.Add(bot);
            }
            SaveAccounts();
            RefreshAccountsDropdown();
            SetStatus("Đã tạo nhanh 5 tài khoản bot farm!", ClrCyan);
        }

        private void DoDeleteAccount()
        {
            if (cmbAccounts.SelectedIndex >= 0 && cmbAccounts.SelectedIndex < accounts.Count)
            {
                string removed = accounts[cmbAccounts.SelectedIndex];
                accounts.RemoveAt(cmbAccounts.SelectedIndex);
                SaveAccounts();
                RefreshAccountsDropdown();
                if (accounts.Count > 0)
                    txtNick.Text = accounts[0];
                else
                    txtNick.Text = "";
                SetStatus("Đã xóa tài khoản: " + removed, ClrRed);
            }
        }

        private void LaunchGame()
        {
            string nick = txtNick.Text.Trim();
            if (string.IsNullOrEmpty(nick) && cmbAccounts.SelectedIndex >= 0 && cmbAccounts.SelectedIndex < accounts.Count)
            {
                nick = accounts[cmbAccounts.SelectedIndex];
                txtNick.Text = nick;
            }

            if (string.IsNullOrEmpty(nick))
            {
                SetStatus("Vui lòng nhập hoặc chọn tên tài khoản!", ClrRed);
                return;
            }

            if (!accounts.Contains(nick))
            {
                accounts.Add(nick);
                SaveAccounts();
                RefreshAccountsDropdown();
            }

            int ram = (int)sldRam.Value;
            bool shouldExit = chkAutoClose.IsChecked == true;
            string modeName = currentMode == 0 ? "Forge 1.12.2" : "Optifine 1.12.2";

            SetStatus(string.Format("Đang mở game ({0}) cho tài khoản {1}...", modeName, nick), ClrSilver);
            btnPlay.IsEnabled = false;

            new Thread(delegate()
            {
                Process proc = (currentMode == 0) ? RunMinecraftForge(nick, ram) : RunMinecraftOptiFine(nick, ram);
                if (proc == null)
                {
                    Application.Current.Dispatcher.Invoke((Action)delegate
                    {
                        btnPlay.IsEnabled = true;
                        this.Show();
                        this.WindowState = WindowState.Normal;
                    });
                    return;
                }

                // ─── KIỂM TRA SỰ CỐ KHỞI CHẠY (CRASH MONITOR TRONG 7 GIÂY ĐẦU) ───
                bool crashed = false;
                int exitCode = 0;
                for (int i = 0; i < 14; i++)
                {
                    Thread.Sleep(500);
                    if (proc.HasExited)
                    {
                        crashed = true;
                        exitCode = proc.ExitCode;
                        try { proc.WaitForExit(1500); } catch { }
                        break;
                    }
                }

                Application.Current.Dispatcher.Invoke((Action)delegate
                {
                    if (crashed && exitCode != 0)
                    {
                        this.Show();
                        this.WindowState = WindowState.Normal;
                        btnPlay.IsEnabled = true;

                        string errSnippet = "";
                        try
                        {
                            string mc = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
                            string lf = System.IO.Path.Combine(mc, "logs", "launch_output.log");
                            if (File.Exists(lf))
                            {
                                string[] lines = File.ReadAllLines(lf);
                                int take = Math.Min(lines.Length, 20);
                                if (take > 0)
                                {
                                    string[] sub = new string[take];
                                    Array.Copy(lines, lines.Length - take, sub, 0, take);
                                    errSnippet = string.Join("\n", sub);
                                }
                            }
                        }
                        catch { }

                        SetStatus(string.Format("✖ Lỗi khởi chạy (Mã: {0})! Xem logs/launch_output.log", exitCode), ClrRed);

                        string msg = string.Format("Game Minecraft thoát bất ngờ với mã lỗi: {0}!\n\n", exitCode);
                        if (!string.IsNullOrEmpty(errSnippet))
                        {
                            msg += "Nhật ký lỗi ghi nhận:\n----------------------------------------\n" + errSnippet + "\n----------------------------------------\n\n";
                        }
                        msg += "Nhật ký đầy đủ được lưu tại:\n%APPDATA%\\.minecraft\\logs\\launch_output.log\n\nNếu máy tính chưa có Java, hãy mở file 'CasxDuoisClient.exe' để tự động giải nén môi trường.";
                        MessageBox.Show(msg, "Lỗi khởi chạy Client", MessageBoxButton.OK, MessageBoxImage.Error);
                    }
                    else
                    {
                        if (shouldExit)
                        {
                            Application.Current.Shutdown();
                        }
                        else
                        {
                            btnPlay.IsEnabled = true;
                            SetStatus(string.Format("✔ Đã mở game thành công cho {0} ({1})!", nick, modeName), ClrGreen);
                        }
                    }
                });
            }).Start();

            if (shouldExit) this.Hide();
        }

        private void LaunchAllAccounts()
        {
            if (accounts.Count == 0)
            {
                SetStatus("Chưa có tài khoản nào trong danh sách!", ClrRed);
                return;
            }

            int ram = (int)sldRam.Value;
            bool shouldExit = chkAutoClose.IsChecked == true;
            string modeName = currentMode == 0 ? "Forge 1.12.2" : "Optifine 1.12.2";

            SetStatus(string.Format("Đang mở {0} tab ({1}) cùng lúc...", accounts.Count, modeName), ClrSilver);
            btnPlay.IsEnabled = false;

            new Thread(delegate()
            {
                for (int i = 0; i < accounts.Count; i++)
                {
                    if (currentMode == 0)
                        RunMinecraftForge(accounts[i], ram);
                    else
                        RunMinecraftOptiFine(accounts[i], ram);

                    if (i < accounts.Count - 1) Thread.Sleep(2000);
                }

                Application.Current.Dispatcher.Invoke((Action)delegate
                {
                    if (shouldExit)
                    {
                        Thread.Sleep(1200);
                        Application.Current.Shutdown();
                    }
                    else
                    {
                        btnPlay.IsEnabled = true;
                        SetStatus(string.Format("✔ Đã mở toàn bộ {0} tab thành công ({1})!", accounts.Count, modeName), ClrGreen);
                    }
                });
            }).Start();

            if (shouldExit) this.Hide();
        }

        private bool CheckJavaInPath(string exe)
        {
            if (string.IsNullOrEmpty(exe)) return false;
            if (File.Exists(exe)) return true;
            try
            {
                ProcessStartInfo psi = new ProcessStartInfo(exe, "-version");
                psi.CreateNoWindow = true;
                psi.UseShellExecute = false;
                using (Process p = Process.Start(psi))
                {
                    p.WaitForExit(2000);
                    return true;
                }
            }
            catch { return false; }
        }

        // ═════════════════════════════════════════════════════════════════
        //  MODE 0: MINECRAFT 1.12.2 MIXIN x FORGE (FULL CLIENT MOD)
        // ═════════════════════════════════════════════════════════════════
        private Process RunMinecraftForge(string username, int ramMB)
        {
            try
            {
                string mcDir = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
                string javaDesc;
                string javaExe = DetectJava(out javaDesc);

                if (!File.Exists(javaExe) && !CheckJavaInPath(javaExe))
                {
                    Application.Current.Dispatcher.Invoke((Action)delegate
                    {
                        MessageBoxResult res = MessageBox.Show(
                            "Máy tính của bạn chưa có Java 8 để khởi chạy Minecraft 1.12.2!\n\n"
                          + "CasxDuois có thể tự động tải và cài đặt Java 8 Portable (~38 MB) về máy ngay bây giờ.\n"
                          + "(Hoàn toàn tự động, dùng riêng cho game, không cần quyền Admin hay cài đặt phức tạp).\n\n"
                          + "Bạn có muốn tải Java 8 tự động ngay bây giờ không?",
                            "Tự động tải Java 8 Portable",
                            MessageBoxButton.YesNo,
                            MessageBoxImage.Question);

                        if (res == MessageBoxResult.Yes)
                        {
                            StartDownloadJava8(delegate()
                            {
                                LaunchGame();
                            });
                        }
                        else
                        {
                            SetStatus("✖ Cần có Java 8 để khởi chạy Minecraft!", ClrRed);
                        }
                    });
                    return null;
                }

                string forgeJsonFile;
                string forgeVDir;
                if (!ResolveForgeVersion(mcDir, out forgeJsonFile, out forgeVDir))
                {
                    Application.Current.Dispatcher.Invoke((Action)delegate
                    {
                        MessageBox.Show("Không tìm thấy dữ liệu Forge 1.12.2 trong thư mục .minecraft!\n\n"
                                      + "Nếu bạn dùng TLauncher:\n"
                                      + "1. Mở TLauncher, tại danh sách phiên bản hãy chọn 'Forge 1.12.2' hoặc 'ForgeOptiFine 1.12.2'.\n"
                                      + "2. Bấm 'Cài đặt' hoặc vào game 1 lần để TLauncher tải đủ thư viện.\n"
                                      + "3. Sau đó mở lại CasxDuois Launcher để chơi ngay!",
                                      "Thiếu gói Forge 1.12.2", MessageBoxButton.OK, MessageBoxImage.Warning);
                        SetStatus("✖ Thiếu dữ liệu Forge 1.12.2!", ClrRed);
                    });
                    return null;
                }

                string jar = ResolveVanillaJar(mcDir, forgeVDir);
                if (string.IsNullOrEmpty(jar) || !File.Exists(jar))
                {
                    Application.Current.Dispatcher.Invoke((Action)delegate
                    {
                        MessageBox.Show("Không tìm thấy file jar Minecraft 1.12.2 trong .minecraft\\versions!\n\n"
                                      + "Vui lòng mở TLauncher và bấm vào phiên bản 1.12.2 (hoặc Forge 1.12.2) để tải game một lần.",
                                      "Thiếu file 1.12.2.jar", MessageBoxButton.OK, MessageBoxImage.Warning);
                        SetStatus("✖ Thiếu file 1.12.2.jar!", ClrRed);
                    });
                    return null;
                }

                string natives = ResolveNatives(mcDir, forgeVDir);
                string vDir = System.IO.Path.GetDirectoryName(jar);
                string jsonFile = System.IO.Path.Combine(vDir, System.IO.Path.GetFileNameWithoutExtension(jar) + ".json");
                if (!File.Exists(jsonFile))
                {
                    string stdJson = System.IO.Path.Combine(mcDir, "versions", "1.12.2", "1.12.2.json");
                    if (File.Exists(stdJson)) jsonFile = stdJson;
                }
                string libBase = System.IO.Path.Combine(mcDir, "libraries");

                StringBuilder cp = new StringBuilder();
                HashSet<string> addedPaths = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

                Action<string> addLib = delegate(string fullPath)
                {
                    if (!string.IsNullOrEmpty(fullPath) && File.Exists(fullPath) && !addedPaths.Contains(fullPath) && !fullPath.Contains("natives"))
                    {
                        cp.Append(fullPath).Append(";");
                        addedPaths.Add(fullPath);
                    }
                };

                // 1. Forge Libraries
                AddLibrariesFromJson(forgeJsonFile, libBase, addLib, null);

                // 2. Vanilla Libraries
                AddLibrariesFromJson(jsonFile, libBase, addLib, null);

                // 3. Forge jar (Quét linh hoạt mọi phiên bản Forge jar trong libraries)
                string[] forgeDirs = {
                    System.IO.Path.Combine(libBase, "net", "minecraftforge", "forge"),
                    forgeVDir
                };
                foreach (string fd in forgeDirs)
                {
                    if (Directory.Exists(fd))
                    {
                        try
                        {
                            foreach (string fj in Directory.GetFiles(fd, "*forge*.jar", SearchOption.AllDirectories))
                            {
                                addLib(fj);
                            }
                        }
                        catch { }
                    }
                }

                // 4. Launchwrapper jars
                string lwDir = System.IO.Path.Combine(libBase, "net", "minecraft", "launchwrapper");
                if (Directory.Exists(lwDir))
                {
                    try
                    {
                        foreach (string lj in Directory.GetFiles(lwDir, "*.jar", SearchOption.AllDirectories))
                        {
                            addLib(lj);
                        }
                    }
                    catch { }
                }

                // 5. Vanilla jar
                addLib(jar);

                // Đảm bảo mod CasxDuois Client luôn có mặt trong thư mục mods
                SyncModAndProfiles();

                // Ensure splash.properties disabled
                try
                {
                    string cfgDir = System.IO.Path.Combine(mcDir, "config");
                    if (!Directory.Exists(cfgDir)) Directory.CreateDirectory(cfgDir);
                    string splashFile = System.IO.Path.Combine(cfgDir, "splash.properties");
                    if (!File.Exists(splashFile))
                    {
                        File.WriteAllText(splashFile, "enabled=false\r\n");
                    }
                }
                catch { }

                string customTitle = username;
                int xms = Math.Min(512, ramMB);
                string launchArgs = string.Format(
                    "-Xms{0}M -Xmx{1}M -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -Dfml.splash=false -Djava.library.path=\"{2}\" -Dminecraft.launcher.brand=CasxDuoisMC -cp \"{3}\" net.minecraft.launchwrapper.Launch --username \"{4}\" --version 1.12.2-forge-14.23.5.2860 --gameDir \"{5}\" --assetsDir \"{6}\" --assetIndex 1.12 --uuid 0 --accessToken 0 --userType legacy --tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker --versionType Forge",
                    xms, ramMB, natives, cp.ToString(), username, mcDir, System.IO.Path.Combine(mcDir, "assets"));

                string logsDir = System.IO.Path.Combine(mcDir, "logs");
                if (!Directory.Exists(logsDir)) Directory.CreateDirectory(logsDir);
                string logFile = System.IO.Path.Combine(logsDir, "launch_output.log");

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = javaExe;
                psi.Arguments = launchArgs;
                psi.WorkingDirectory = mcDir;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;
                psi.RedirectStandardOutput = true;
                psi.RedirectStandardError = true;

                Process gameProcess = new Process();
                gameProcess.StartInfo = psi;

                FileStream fs = new FileStream(logFile, FileMode.Create, FileAccess.Write, FileShare.ReadWrite);
                StreamWriter sw = new StreamWriter(fs, Encoding.UTF8) { AutoFlush = true };

                gameProcess.OutputDataReceived += (s, e) => { if (e.Data != null) sw.WriteLine(e.Data); };
                gameProcess.ErrorDataReceived += (s, e) => { if (e.Data != null) sw.WriteLine("[STDERR] " + e.Data); };

                gameProcess.Start();
                gameProcess.BeginOutputReadLine();
                gameProcess.BeginErrorReadLine();

                MonitorWindowTitle(gameProcess, customTitle);
                return gameProcess;
            }
            catch (Exception ex)
            {
                Application.Current.Dispatcher.Invoke((Action)delegate
                {
                    SetStatus("Lỗi mở game Forge: " + ex.Message, ClrRed);
                    MessageBox.Show("Không thể khởi động tiến trình Java:\n" + ex.Message + "\n\nVui lòng kiểm tra lại môi trường Java.", "Lỗi khởi chạy", MessageBoxButton.OK, MessageBoxImage.Error);
                });
                return null;
            }
        }

        // ═════════════════════════════════════════════════════════════════
        //  MODE 1: MINECRAFT 1.12.2 OPTIFINE STANDALONE (SIÊU LITE AFK)
        // ═════════════════════════════════════════════════════════════════
        private Process RunMinecraftOptiFine(string username, int ramMB)
        {
            try
            {
                string mcDir = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
                string javaDesc;
                string javaExe = DetectJava(out javaDesc);

                if (!File.Exists(javaExe) && !CheckJavaInPath(javaExe))
                {
                    Application.Current.Dispatcher.Invoke((Action)delegate
                    {
                        MessageBoxResult res = MessageBox.Show(
                            "Máy tính của bạn chưa có Java 8 để khởi chạy Minecraft 1.12.2!\n\n"
                          + "CasxDuois có thể tự động tải và cài đặt Java 8 Portable (~38 MB) về máy ngay bây giờ.\n"
                          + "(Hoàn toàn tự động, dùng riêng cho game, không cần quyền Admin hay cài đặt phức tạp).\n\n"
                          + "Bạn có muốn tải Java 8 tự động ngay bây giờ không?",
                            "Tự động tải Java 8 Portable",
                            MessageBoxButton.YesNo,
                            MessageBoxImage.Question);

                        if (res == MessageBoxResult.Yes)
                        {
                            StartDownloadJava8(delegate()
                            {
                                LaunchGame();
                            });
                        }
                        else
                        {
                            SetStatus("✖ Cần có Java 8 để khởi chạy Minecraft!", ClrRed);
                        }
                    });
                    return null;
                }

                string optiJson;
                string optiVDir;
                if (!ResolveOptiFineVersion(mcDir, out optiJson, out optiVDir))
                {
                    Application.Current.Dispatcher.Invoke((Action)delegate
                    {
                        MessageBox.Show("Không tìm thấy dữ liệu OptiFine 1.12.2 trong thư mục .minecraft!\n\n"
                                      + "Nếu bạn dùng TLauncher, vui lòng chọn cài đặt phiên bản 'OptiFine 1.12.2' trong danh sách của TLauncher một lần,\n"
                                      + "hoặc mở file 'CasxDuoisClient.exe' để tự động giải nén tài nguyên.",
                                      "Thiếu OptiFine 1.12.2", MessageBoxButton.OK, MessageBoxImage.Warning);
                        SetStatus("✖ Thiếu OptiFine 1.12.2!", ClrRed);
                    });
                    return null;
                }

                string jar = ResolveVanillaJar(mcDir, optiVDir);
                if (string.IsNullOrEmpty(jar) || !File.Exists(jar))
                {
                    Application.Current.Dispatcher.Invoke((Action)delegate
                    {
                        MessageBox.Show("Không tìm thấy file jar Minecraft 1.12.2 trong .minecraft\\versions!\n\n"
                                      + "Vui lòng mở TLauncher và bấm vào phiên bản 1.12.2 (hoặc OptiFine 1.12.2) để tải game.",
                                      "Thiếu file 1.12.2.jar", MessageBoxButton.OK, MessageBoxImage.Warning);
                        SetStatus("✖ Thiếu file 1.12.2.jar!", ClrRed);
                    });
                    return null;
                }

                string natives = ResolveNatives(mcDir, optiVDir);
                string vDir = System.IO.Path.GetDirectoryName(jar);
                string jsonFile = System.IO.Path.Combine(vDir, System.IO.Path.GetFileNameWithoutExtension(jar) + ".json");
                if (!File.Exists(jsonFile))
                {
                    string stdJson = System.IO.Path.Combine(mcDir, "versions", "1.12.2", "1.12.2.json");
                    if (File.Exists(stdJson)) jsonFile = stdJson;
                }
                string libBase = System.IO.Path.Combine(mcDir, "libraries");

                StringBuilder cp = new StringBuilder();
                HashSet<string> addedPaths = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

                Action<string> addLib = delegate(string fullPath)
                {
                    if (!string.IsNullOrEmpty(fullPath) && File.Exists(fullPath) && !addedPaths.Contains(fullPath) && !fullPath.Contains("natives"))
                    {
                        cp.Append(fullPath).Append(";");
                        addedPaths.Add(fullPath);
                    }
                };

                // Đảm bảo mod và thư viện OptiFine luôn được đồng bộ
                SyncModAndProfiles();

                string localOpti = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "OptiFine 1.12.2 - CasxDuois Model");
                if (!Directory.Exists(localOpti)) localOpti = @"d:\CasxDuois\OptiFine 1.12.2 - CasxDuois Model";
                string localLibs = System.IO.Path.Combine(localOpti, "libraries");

                // 1. OptiFine JAR (chỉ nạp đúng bản 1.12.2, tuyệt đối tránh nạp bản 1.16/1.20/1.21)
                string[] optiJarCandidates = {
                    System.IO.Path.Combine(libBase, "optifine", "OptiFine", "1.12.2_HD_U_G5", "OptiFine-1.12.2_HD_U_G5.jar"),
                    System.IO.Path.Combine(localLibs, "optifine", "OptiFine", "1.12.2_HD_U_G5", "OptiFine-1.12.2_HD_U_G5.jar"),
                    System.IO.Path.Combine(localOpti, "OptiFine-1.12.2_HD_U_G5.jar"),
                    System.IO.Path.Combine(mcDir, "mods", "OptiFine_1.12.2_HD_U_G5.jar")
                };
                bool optiFound = false;
                foreach (string oj in optiJarCandidates)
                {
                    if (File.Exists(oj)) { addLib(oj); optiFound = true; break; }
                }
                if (!optiFound)
                {
                    string optiLibDir = System.IO.Path.Combine(libBase, "optifine", "OptiFine");
                    if (Directory.Exists(optiLibDir))
                    {
                        try
                        {
                            foreach (string oj in Directory.GetFiles(optiLibDir, "*1.12.2*.jar", SearchOption.AllDirectories))
                            {
                                addLib(oj);
                                break;
                            }
                        }
                        catch { }
                    }
                }

                // 2. Launchwrapper JAR (Chỉ cần nạp 1 bản duy nhất)
                string[] lwCandidates = {
                    System.IO.Path.Combine(libBase, "optifine", "launchwrapper", "2.2", "launchwrapper-2.2.jar"),
                    System.IO.Path.Combine(localLibs, "optifine", "launchwrapper", "2.2", "launchwrapper-2.2.jar"),
                    System.IO.Path.Combine(libBase, "net", "minecraft", "launchwrapper", "1.12", "launchwrapper-1.12.jar")
                };
                foreach (string lw in lwCandidates)
                {
                    if (File.Exists(lw)) { addLib(lw); break; }
                }

                // 3. OptiFine version libraries (nạp toàn bộ thư viện 1.12.2 chuẩn)
                if (File.Exists(optiJson))
                {
                    AddLibrariesFromJson(optiJson, libBase, addLib, localLibs);
                }
                else if (File.Exists(jsonFile))
                {
                    AddLibrariesFromJson(jsonFile, libBase, addLib, null);
                }

                // 4. Vanilla 1.12.2 jar
                addLib(jar);

                string customTitle = username;
                int xms = Math.Min(512, ramMB);
                string launchArgs = string.Format(
                    "-Xms{0}M -Xmx{1}M -XX:+UseG1GC -Djava.library.path=\"{2}\" -Dminecraft.launcher.brand=CasxDuoisMC -cp \"{3}\" net.minecraft.launchwrapper.Launch --username \"{4}\" --version 1.12.2-OptiFine --gameDir \"{5}\" --assetsDir \"{6}\" --assetIndex 1.12 --uuid 0 --accessToken 0 --userType legacy --tweakClass optifine.OptiFineTweaker",
                    xms, ramMB, natives, cp.ToString(), username, mcDir, System.IO.Path.Combine(mcDir, "assets"));

                string logsDir = System.IO.Path.Combine(mcDir, "logs");
                if (!Directory.Exists(logsDir)) Directory.CreateDirectory(logsDir);
                string logFile = System.IO.Path.Combine(logsDir, "launch_output.log");

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = javaExe;
                psi.Arguments = launchArgs;
                psi.WorkingDirectory = mcDir;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;
                psi.RedirectStandardOutput = true;
                psi.RedirectStandardError = true;

                Process gameProcess = new Process();
                gameProcess.StartInfo = psi;

                FileStream fs = new FileStream(logFile, FileMode.Create, FileAccess.Write, FileShare.ReadWrite);
                StreamWriter sw = new StreamWriter(fs, Encoding.UTF8) { AutoFlush = true };

                gameProcess.OutputDataReceived += (s, e) => { if (e.Data != null) sw.WriteLine(e.Data); };
                gameProcess.ErrorDataReceived += (s, e) => { if (e.Data != null) sw.WriteLine("[STDERR] " + e.Data); };

                gameProcess.Start();
                gameProcess.BeginOutputReadLine();
                gameProcess.BeginErrorReadLine();

                MonitorWindowTitle(gameProcess, customTitle);
                return gameProcess;
            }
            catch (Exception ex)
            {
                Application.Current.Dispatcher.Invoke((Action)delegate
                {
                    SetStatus("Lỗi mở game OptiFine: " + ex.Message, ClrRed);
                    MessageBox.Show("Không thể khởi động tiến trình Java:\n" + ex.Message + "\n\nVui lòng kiểm tra lại môi trường Java.", "Lỗi khởi chạy", MessageBoxButton.OK, MessageBoxImage.Error);
                });
                return null;
            }
        }


        // ─── TỰ ĐỘNG ĐẶT TIÊU ĐỀ CỬA SỔ GAME LÀ CasxDuoisMC_<name> ───
        private void MonitorWindowTitle(Process gameProcess, string customTitle)
        {
            if (gameProcess == null) return;
            new Thread(delegate()
            {
                for (int attempt = 0; attempt < 30; attempt++)
                {
                    Thread.Sleep(500);
                    if (gameProcess.HasExited) break;

                    IntPtr hWnd = gameProcess.MainWindowHandle;
                    if (hWnd != IntPtr.Zero)
                    {
                        SetWindowText(hWnd, customTitle);
                    }
                    else
                    {
                        foreach (ProcessThread thread in gameProcess.Threads)
                        {
                            EnumThreadWindows(thread.Id, delegate(IntPtr handle, IntPtr lp)
                            {
                                SetWindowText(handle, customTitle);
                                return true;
                            }, IntPtr.Zero);
                        }
                    }
                }
            }).Start();
        }

        // ═════════════════════════════════════════════════════════════════
        //  HELPERS & CONFIG
        // ═════════════════════════════════════════════════════════════════
        private void SetStatus(string msg, Color clr)
        {
            lblStatus.Text = msg;
            lblStatus.Foreground = new SolidColorBrush(clr);
        }

        private void LoadAccounts()
        {
            if (!File.Exists(accountsFile))
            {
                accounts.Add("CasxDuoisPlayer");
                SaveAccounts();
                return;
            }
            accounts.Clear();
            foreach (string l in File.ReadAllLines(accountsFile))
            {
                if (!string.IsNullOrEmpty(l.Trim())) accounts.Add(l.Trim());
            }
        }

        private void SaveAccounts()
        {
            File.WriteAllLines(accountsFile, accounts.ToArray());
        }

        private void LoadConfig()
        {
            try
            {
                if (File.Exists(configFile))
                {
                    string txt = File.ReadAllText(configFile);
                    Match mMode = Regex.Match(txt, "\"mode\":\\s*(\\d+)");
                    if (mMode.Success)
                    {
                        int m;
                        if (int.TryParse(mMode.Groups[1].Value, out m))
                        {
                            currentMode = (m == 1) ? 1 : 0;
                        }
                    }

                    Match mRam = Regex.Match(txt, "\"ram\":\\s*(\\d+)");
                    if (mRam.Success)
                    {
                        int r;
                        if (int.TryParse(mRam.Groups[1].Value, out r) && sldRam != null)
                        {
                            sldRam.Value = Math.Max(512, Math.Min(4096, r));
                        }
                    }

                    Match mClose = Regex.Match(txt, "\"auto_close\":\\s*(true|false)");
                    if (mClose.Success && chkAutoClose != null)
                    {
                        chkAutoClose.IsChecked = mClose.Groups[1].Value == "true";
                    }
                }
            }
            catch { }
        }

        private void SaveConfig()
        {
            try
            {
                int r = sldRam != null ? (int)sldRam.Value : 1024;
                bool ac = chkAutoClose != null && chkAutoClose.IsChecked == true;
                string json = string.Format("{{\"mode\": {0}, \"ram\": {1}, \"auto_close\": {2}}}", currentMode, r, ac ? "true" : "false");
                File.WriteAllText(configFile, json);
            }
            catch { }
        }

        private static bool IsJava8(string exePath)
        {
            if (string.IsNullOrEmpty(exePath) || !File.Exists(exePath)) return false;
            try
            {
                FileVersionInfo vi = FileVersionInfo.GetVersionInfo(exePath);
                if (vi.ProductMajorPart == 8 || vi.FileMajorPart == 8) return true;
                if (!string.IsNullOrEmpty(vi.ProductVersion) && (vi.ProductVersion.StartsWith("8.") || vi.ProductVersion.StartsWith("1.8."))) return true;
                if (!string.IsNullOrEmpty(vi.FileVersion) && (vi.FileVersion.StartsWith("8.") || vi.FileVersion.StartsWith("1.8."))) return true;
                string lower = exePath.ToLowerInvariant();
                if (lower.Contains("jre-legacy") || lower.Contains("1.8.0") || lower.Contains("jdk-8") || lower.Contains("jre-8") || lower.Contains("zulu-8")) return true;
            }
            catch { }
            return false;
        }

        private static string FindJava8InDir(string baseDir)
        {
            if (string.IsNullOrEmpty(baseDir) || !Directory.Exists(baseDir)) return null;
            try
            {
                string direct = System.IO.Path.Combine(baseDir, "bin", "javaw.exe");
                if (IsJava8(direct)) return direct;
                direct = System.IO.Path.Combine(baseDir, "javaw.exe");
                if (IsJava8(direct)) return direct;

                foreach (string d in Directory.GetDirectories(baseDir))
                {
                    string p = System.IO.Path.Combine(d, "bin", "javaw.exe");
                    if (IsJava8(p)) return p;
                    p = System.IO.Path.Combine(d, "javaw.exe");
                    if (IsJava8(p)) return p;

                    try
                    {
                        foreach (string sub in Directory.GetDirectories(d))
                        {
                            string sp = System.IO.Path.Combine(sub, "bin", "javaw.exe");
                            if (IsJava8(sp)) return sp;
                            sp = System.IO.Path.Combine(sub, "javaw.exe");
                            if (IsJava8(sp)) return sp;

                            foreach (string s2 in Directory.GetDirectories(sub))
                            {
                                string s2p = System.IO.Path.Combine(s2, "bin", "javaw.exe");
                                if (IsJava8(s2p)) return s2p;
                            }
                        }
                    }
                    catch { }
                }
            }
            catch { }
            return null;
        }

        private string DetectJava()
        {
            string desc;
            return DetectJava(out desc);
        }

        private string DetectJava(out string javaDesc)
        {
            string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            string mcDir = System.IO.Path.Combine(appData, ".minecraft");

            // 1. Portable JRE tích hợp sẵn
            string portableJre = System.IO.Path.Combine(mcDir, "runtime", "jre-x64", "bin", "javaw.exe");
            if (IsJava8(portableJre))
            {
                javaDesc = "Java 8 Portable (Tích hợp)";
                return portableJre;
            }
            string localJre = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "runtime", "jre-x64", "bin", "javaw.exe");
            if (IsJava8(localJre))
            {
                javaDesc = "Java 8 Portable (Thư mục Client)";
                return localJre;
            }

            // 2. TLauncher Java 8 (Rất phổ biến tại VN, jre-legacy là chuẩn Java 8)
            string[] tlauncherPaths = {
                System.IO.Path.Combine(appData, ".tlauncher", "legacy", "Minecraft", "jre", "jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe"),
                System.IO.Path.Combine(appData, ".tlauncher", "cache", "jvms", "jre-legacy", "bin", "javaw.exe")
            };
            foreach (string tp in tlauncherPaths)
            {
                if (IsJava8(tp))
                {
                    javaDesc = "Java 8 (Có sẵn từ TLauncher)";
                    return tp;
                }
            }

            string tlScan = FindJava8InDir(System.IO.Path.Combine(appData, ".tlauncher", "legacy", "Minecraft", "jre"));
            if (!string.IsNullOrEmpty(tlScan))
            {
                javaDesc = "Java 8 (Có sẵn từ TLauncher)";
                return tlScan;
            }
            tlScan = FindJava8InDir(System.IO.Path.Combine(appData, ".tlauncher"));
            if (!string.IsNullOrEmpty(tlScan))
            {
                javaDesc = "Java 8 (Có sẵn từ TLauncher)";
                return tlScan;
            }

            // 3. Minecraft Launcher chính thức (Mojang Runtime Java 8)
            string[] mojangPaths = {
                System.IO.Path.Combine(mcDir, "runtime", "jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe"),
                System.IO.Path.Combine(mcDir, "runtime", "java-runtime-alpha", "windows-x64", "java-runtime-alpha", "bin", "javaw.exe"),
                System.IO.Path.Combine(mcDir, "runtime", "1.8.0", "bin", "javaw.exe"),
                System.IO.Path.Combine(localAppData, "Packages", "Microsoft.4297127D64EC6_8wekyb3d8bbwe", "LocalCache", "Local", "runtime", "jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe"),
                @"C:\Program Files (x86)\Minecraft Launcher\runtime\jre-legacy\windows-x64\jre-legacy\bin\javaw.exe",
                @"C:\Program Files\Minecraft Launcher\runtime\jre-legacy\windows-x64\jre-legacy\bin\javaw.exe"
            };
            foreach (string mp in mojangPaths)
            {
                if (IsJava8(mp))
                {
                    javaDesc = "Java 8 (Minecraft Launcher)";
                    return mp;
                }
            }
            string mcScan = FindJava8InDir(System.IO.Path.Combine(mcDir, "runtime"));
            if (!string.IsNullOrEmpty(mcScan))
            {
                javaDesc = "Java 8 (Minecraft Runtime)";
                return mcScan;
            }

            // 4. Các Launcher/Client khác (Lunar, CurseForge, Badlion, Prism)
            string curse = System.IO.Path.Combine(userProfile, "curseforge", "minecraft", "Install", "runtime", "jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe");
            if (IsJava8(curse))
            {
                javaDesc = "Java 8 (CurseForge)";
                return curse;
            }
            string lunar = FindJava8InDir(System.IO.Path.Combine(userProfile, ".lunarclient", "offline", "multiver"));
            if (!string.IsNullOrEmpty(lunar))
            {
                javaDesc = "Java 8 (Lunar Client)";
                return lunar;
            }
            lunar = FindJava8InDir(System.IO.Path.Combine(userProfile, ".lunarclient", "jre"));
            if (!string.IsNullOrEmpty(lunar))
            {
                javaDesc = "Java 8 (Lunar Client)";
                return lunar;
            }
            string prism = FindJava8InDir(System.IO.Path.Combine(appData, "PrismLauncher", "java"));
            if (!string.IsNullOrEmpty(prism))
            {
                javaDesc = "Java 8 (Prism Launcher)";
                return prism;
            }

            // 5. Eclipse Adoptium / Temurin JDK 8
            string[] adoptiumBases = {
                @"C:\Program Files\Eclipse Adoptium",
                @"C:\Program Files (x86)\Eclipse Adoptium"
            };
            foreach (string aBase in adoptiumBases)
            {
                string aFound = FindJava8InDir(aBase);
                if (!string.IsNullOrEmpty(aFound))
                {
                    javaDesc = "Eclipse Adoptium JDK 8";
                    return aFound;
                }
            }

            // 6. Cài đặt hệ thống (Oracle, BellSoft, Zulu, Microsoft, Corretto)
            string[] sysBases = {
                @"C:\Program Files\Java",
                @"C:\Program Files (x86)\Java",
                @"C:\Program Files\BellSoft",
                @"C:\Program Files (x86)\BellSoft",
                @"C:\Program Files\Zulu",
                @"C:\Program Files (x86)\Zulu",
                @"C:\Program Files\Microsoft",
                @"C:\Program Files\Amazon Corretto"
            };
            foreach (string sb in sysBases)
            {
                string sFound = FindJava8InDir(sb);
                if (!string.IsNullOrEmpty(sFound))
                {
                    javaDesc = "Java 8 (Hệ thống)";
                    return sFound;
                }
            }

            // 7. Kiểm tra Windows Registry
            try
            {
                using (Microsoft.Win32.RegistryKey rk = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(@"SOFTWARE\JavaSoft\Java Runtime Environment\1.8"))
                {
                    if (rk != null)
                    {
                        object jh = rk.GetValue("JavaHome");
                        if (jh != null)
                        {
                            string p = System.IO.Path.Combine(jh.ToString(), "bin", "javaw.exe");
                            if (IsJava8(p))
                            {
                                javaDesc = "Java 8 (Registry)";
                                return p;
                            }
                        }
                    }
                }
            }
            catch { }

            // 8. Kiểm tra PATH môi trường
            string pathEnv = Environment.GetEnvironmentVariable("PATH");
            if (!string.IsNullOrEmpty(pathEnv))
            {
                foreach (string part in pathEnv.Split(';'))
                {
                    string trimmed = part.Trim();
                    if (string.IsNullOrEmpty(trimmed)) continue;
                    try
                    {
                        string candidate = System.IO.Path.Combine(trimmed, "javaw.exe");
                        if (IsJava8(candidate))
                        {
                            javaDesc = "Java 8 (System PATH)";
                            return candidate;
                        }
                    }
                    catch { }
                }
            }

            javaDesc = "Chưa phát hiện Java 8";
            return "javaw.exe";
        }

        private void RefreshJavaDetection()
        {
            try
            {
                string desc;
                string exe = DetectJava(out desc);
                detectedJavaExe = exe;
                detectedJavaDesc = desc;

                if (lblJavaInfo != null)
                {
                    bool hasJava = File.Exists(exe) || CheckJavaInPath(exe);
                    if (hasJava)
                    {
                        lblJavaInfo.Text = "☕ " + desc;
                        lblJavaInfo.Foreground = new SolidColorBrush(ClrGreen);
                        if (btnDownloadJava != null) btnDownloadJava.Visibility = Visibility.Collapsed;
                    }
                    else
                    {
                        lblJavaInfo.Text = "☕ Chưa phát hiện Java 8";
                        lblJavaInfo.Foreground = new SolidColorBrush(ClrRed);
                        if (btnDownloadJava != null && !isDownloadingJava) btnDownloadJava.Visibility = Visibility.Visible;
                    }
                }
            }
            catch { }
        }

        public void StartDownloadJava8(Action onComplete = null)
        {
            if (isDownloadingJava) return;
            isDownloadingJava = true;

            if (btnDownloadJava != null) btnDownloadJava.Visibility = Visibility.Collapsed;
            if (spDownload != null) spDownload.Visibility = Visibility.Visible;
            if (lblDownloadStatus != null) lblDownloadStatus.Text = "Đang kết nối máy chủ tải Java 8 Portable (~38 MB)...";
            if (prgDownload != null) { prgDownload.Value = 0; prgDownload.IsIndeterminate = true; }
            if (btnPlay != null) btnPlay.IsEnabled = false;

            string tempZip = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "CasxDuois_Adoptium_Java8.zip");
            string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string targetJreDir = System.IO.Path.Combine(appData, ".minecraft", "runtime", "jre-x64");

            Thread dlThread = new Thread(delegate()
            {
                try
                {
                    ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072 | SecurityProtocolType.Tls;
                    using (WebClient client = new WebClient())
                    {
                        client.Headers.Add("User-Agent", "CasxDuoisLauncher/1.0");

                        client.DownloadProgressChanged += (s, e) =>
                        {
                            Dispatcher.Invoke((Action)delegate
                            {
                                if (prgDownload != null)
                                {
                                    prgDownload.IsIndeterminate = false;
                                    prgDownload.Value = e.ProgressPercentage;
                                }
                                if (lblDownloadStatus != null)
                                {
                                    double mbReceived = e.BytesReceived / 1048576.0;
                                    double mbTotal = e.TotalBytesToReceive > 0 ? e.TotalBytesToReceive / 1048576.0 : 38.2;
                                    lblDownloadStatus.Text = string.Format("Đang tải Java 8 Portable: {0:F1} MB / {1:F1} MB ({2}%)", mbReceived, mbTotal, e.ProgressPercentage);
                                }
                            });
                        };

                        client.DownloadFileCompleted += (s, e) =>
                        {
                            if (e.Error != null)
                            {
                                Dispatcher.Invoke((Action)delegate
                                {
                                    isDownloadingJava = false;
                                    if (spDownload != null) spDownload.Visibility = Visibility.Collapsed;
                                    if (btnPlay != null) btnPlay.IsEnabled = true;
                                    RefreshJavaDetection();
                                    MessageBox.Show("Lỗi trong quá trình tải Java 8:\n" + e.Error.Message + "\n\nVui lòng kiểm tra lại kết nối mạng.", "Lỗi tải Java", MessageBoxButton.OK, MessageBoxImage.Error);
                                });
                                return;
                            }

                            // Giải nén zip
                            new Thread(delegate()
                            {
                                try
                                {
                                    Dispatcher.Invoke((Action)delegate
                                    {
                                        if (lblDownloadStatus != null) lblDownloadStatus.Text = "Đang giải nén và cấu hình Java 8 Portable...";
                                        if (prgDownload != null) { prgDownload.Value = 100; prgDownload.IsIndeterminate = true; }
                                    });

                                    if (!Directory.Exists(targetJreDir)) Directory.CreateDirectory(targetJreDir);

                                    using (ZipArchive archive = ZipFile.OpenRead(tempZip))
                                    {
                                        string rootPrefix = "";
                                        foreach (ZipArchiveEntry ent in archive.Entries)
                                        {
                                            string fn = ent.FullName.Replace('\\', '/');
                                            int bIdx = fn.IndexOf("/bin/", StringComparison.OrdinalIgnoreCase);
                                            if (bIdx >= 0)
                                            {
                                                rootPrefix = fn.Substring(0, bIdx + 1);
                                                break;
                                            }
                                        }

                                        foreach (ZipArchiveEntry entry in archive.Entries)
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

                                    try { File.Delete(tempZip); } catch { }

                                    Dispatcher.Invoke((Action)delegate
                                    {
                                        isDownloadingJava = false;
                                        if (spDownload != null) spDownload.Visibility = Visibility.Collapsed;
                                        if (btnPlay != null) btnPlay.IsEnabled = true;
                                        RefreshJavaDetection();
                                        SetStatus("✔ Đã cài đặt Java 8 Portable thành công!", ClrGreen);
                                        if (onComplete != null) onComplete();
                                    });
                                }
                                catch (Exception exExtract)
                                {
                                    Dispatcher.Invoke((Action)delegate
                                    {
                                        isDownloadingJava = false;
                                        if (spDownload != null) spDownload.Visibility = Visibility.Collapsed;
                                        if (btnPlay != null) btnPlay.IsEnabled = true;
                                        RefreshJavaDetection();
                                        MessageBox.Show("Lỗi giải nén Java 8:\n" + exExtract.Message, "Lỗi giải nén", MessageBoxButton.OK, MessageBoxImage.Error);
                                    });
                                }
                            }).Start();
                        };

                        string url = "https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse";
                        client.DownloadFileAsync(new Uri(url), tempZip);
                    }
                }
                catch (Exception ex)
                {
                    Dispatcher.Invoke((Action)delegate
                    {
                        isDownloadingJava = false;
                        if (spDownload != null) spDownload.Visibility = Visibility.Collapsed;
                        if (btnPlay != null) btnPlay.IsEnabled = true;
                        RefreshJavaDetection();
                        MessageBox.Show("Không thể bắt đầu tải Java 8:\n" + ex.Message, "Lỗi tải Java", MessageBoxButton.OK, MessageBoxImage.Error);
                    });
                }
            });
            dlThread.Start();
        }

        private static void DeleteOldMixinJars(string mcDir, string keepFile = null)
        {
            try
            {
                string modsDir = System.IO.Path.Combine(mcDir, "mods");
                if (Directory.Exists(modsDir))
                {
                    string keepNorm = string.IsNullOrEmpty(keepFile) ? "" : System.IO.Path.GetFullPath(keepFile).ToLowerInvariant();
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
                            // Nhận diện và xóa triệt để các mod Mixin cũ: CasxDuois, Ascend, hoặc các mod mixin client khác
                            if (fn.Contains("casxduois") || fn.Contains("ascend") || (fn.Contains("mixin") && !fn.Contains("mixinbooter")))
                            {
                                try { File.SetAttributes(f, FileAttributes.Normal); } catch { }
                                File.Delete(f);
                            }
                        }
                        catch { }
                    }
                }

                // Xóa thư mục .mixin.out (cache debug transformed classes) nếu tồn tại
                string mixinOut = System.IO.Path.Combine(mcDir, ".mixin.out");
                if (Directory.Exists(mixinOut))
                {
                    try { Directory.Delete(mixinOut, true); } catch { }
                }
            }
            catch { }
        }

        private void SyncModAndProfiles()
        {
            try
            {
                string mcDir = System.IO.Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
                string modsDir = System.IO.Path.Combine(mcDir, "mods");
                if (!Directory.Exists(modsDir)) Directory.CreateDirectory(modsDir);

                string launcherVersionFile = System.IO.Path.Combine(configDir, "launcher_app.version");
                string curExe = "";
                try
                {
                    curExe = Assembly.GetExecutingAssembly().Location;
                    if (string.IsNullOrEmpty(curExe) || !File.Exists(curExe))
                    {
                        curExe = Process.GetCurrentProcess().MainModule.FileName;
                    }
                }
                catch { }

                string curStamp = "";
                if (!string.IsNullOrEmpty(curExe) && File.Exists(curExe))
                {
                    FileInfo fe = new FileInfo(curExe);
                    curStamp = string.Format("{0}_{1}", fe.LastWriteTimeUtc.Ticks, fe.Length);
                }

                bool isNewLauncher = false;
                if (!string.IsNullOrEmpty(curStamp))
                {
                    if (!File.Exists(launcherVersionFile) || File.ReadAllText(launcherVersionFile).Trim() != curStamp)
                    {
                        isNewLauncher = true;
                    }
                }

                string baseDir = AppDomain.CurrentDomain.BaseDirectory;
                string[] candidates = {
                    System.IO.Path.Combine(baseDir, "build", "libs", "CasxDuoisMCClient-1.0.0-LITE.jar"),
                    System.IO.Path.Combine(baseDir, "build", "libs", "CasxDuoisMCClient-1.0.0-reobf.jar"),
                    System.IO.Path.Combine(baseDir, "CasxDuoisMCClient-1.0.0-LITE.jar"),
                    System.IO.Path.Combine(baseDir, "CasxDuoisMCClient-1.0.0.jar"),
                    @"d:\CasxDuois\build\libs\CasxDuoisMCClient-1.0.0-LITE.jar",
                    @"d:\CasxDuois\build\libs\CasxDuoisMCClient-1.0.0-reobf.jar"
                };

                string foundCandidate = null;
                foreach (string c in candidates)
                {
                    if (File.Exists(c))
                    {
                        foundCandidate = c;
                        break;
                    }
                }

                string targetMod = System.IO.Path.Combine(modsDir, "CasxDuoisMCClient-1.0.0.jar");

                if (foundCandidate != null)
                {
                    FileInfo cInfo = new FileInfo(foundCandidate);
                    bool needUpdate = !File.Exists(targetMod);
                    if (!needUpdate)
                    {
                        FileInfo tInfo = new FileInfo(targetMod);
                        if (cInfo.Length != tInfo.Length || cInfo.LastWriteTimeUtc > tInfo.LastWriteTimeUtc)
                        {
                            needUpdate = true;
                        }
                    }

                    // Nếu là app/launcher bản mới hoặc mod JAR có cập nhật: Xóa sạch mixin cũ rồi mới nạp bản mới
                    if (needUpdate || isNewLauncher)
                    {
                        DeleteOldMixinJars(mcDir, null);
                        try
                        {
                            File.Copy(foundCandidate, targetMod, true);
                            if (!string.IsNullOrEmpty(curStamp))
                            {
                                File.WriteAllText(launcherVersionFile, curStamp);
                            }
                        }
                        catch { }
                    }
                    else
                    {
                        // Giữ lại targetMod hợp lệ, xóa sạch các file mixin cũ/rác khác
                        DeleteOldMixinJars(mcDir, targetMod);
                    }
                }
                else
                {
                    // Trường hợp launcher độc lập: nếu là launcher bản mới, ghi nhận stamp và dọn file thừa
                    if (isNewLauncher && !string.IsNullOrEmpty(curStamp))
                    {
                        try { File.WriteAllText(launcherVersionFile, curStamp); } catch { }
                    }
                    DeleteOldMixinJars(mcDir, File.Exists(targetMod) ? targetMod : null);
                }

                // Cài đặt / Đồng bộ Profile OptiFine CasxDuois vào .minecraft/versions để TLauncher nhận diện ngay
                string localOptiProfile = System.IO.Path.Combine(baseDir, "OptiFine 1.12.2 - CasxDuois Model");
                if (!Directory.Exists(localOptiProfile)) localOptiProfile = @"d:\CasxDuois\OptiFine 1.12.2 - CasxDuois Model";
                if (Directory.Exists(localOptiProfile))
                {
                    string targetProfileDir = System.IO.Path.Combine(mcDir, "versions", "OptiFine 1.12.2 - CasxDuois Model");
                    if (!Directory.Exists(targetProfileDir)) Directory.CreateDirectory(targetProfileDir);

                    string srcJar = System.IO.Path.Combine(localOptiProfile, "OptiFine 1.12.2.jar");
                    string srcJson = System.IO.Path.Combine(localOptiProfile, "OptiFine 1.12.2.json");
                    string targetJar = System.IO.Path.Combine(targetProfileDir, "OptiFine 1.12.2 - CasxDuois Model.jar");
                    string targetJson = System.IO.Path.Combine(targetProfileDir, "OptiFine 1.12.2 - CasxDuois Model.json");

                    if (File.Exists(srcJar) && (!File.Exists(targetJar) || (new FileInfo(srcJar).Length != new FileInfo(targetJar).Length)))
                    {
                        File.Copy(srcJar, targetJar, true);
                    }
                    if (File.Exists(srcJson) && !File.Exists(targetJson))
                    {
                        string jsonTxt = File.ReadAllText(srcJson);
                        jsonTxt = jsonTxt.Replace("\"id\": \"OptiFine 1.12.2\"", "\"id\": \"OptiFine 1.12.2 - CasxDuois Model\"");
                        File.WriteAllText(targetJson, jsonTxt);
                    }

                    // Copy natives nếu có
                    string srcNatives = System.IO.Path.Combine(localOptiProfile, "natives");
                    if (Directory.Exists(srcNatives))
                    {
                        string targetNatives = System.IO.Path.Combine(targetProfileDir, "natives");
                        if (!Directory.Exists(targetNatives)) Directory.CreateDirectory(targetNatives);
                        foreach (string dll in Directory.GetFiles(srcNatives, "*.dll"))
                        {
                            string dName = System.IO.Path.GetFileName(dll);
                            string dstDll = System.IO.Path.Combine(targetNatives, dName);
                            if (!File.Exists(dstDll)) File.Copy(dll, dstDll, true);
                        }
                    }

                    // Copy libraries nếu có (OptiFine jar, launchwrapper jar)
                    string srcLibs = System.IO.Path.Combine(localOptiProfile, "libraries");
                    if (Directory.Exists(srcLibs))
                    {
                        foreach (string file in Directory.GetFiles(srcLibs, "*.*", SearchOption.AllDirectories))
                        {
                            string rel = file.Substring(srcLibs.Length).TrimStart('\\', '/');
                            string dst = System.IO.Path.Combine(mcDir, "libraries", rel);
                            string dstFolder = System.IO.Path.GetDirectoryName(dst);
                            if (!Directory.Exists(dstFolder)) Directory.CreateDirectory(dstFolder);
                            if (!File.Exists(dst) || (new FileInfo(file).Length != new FileInfo(dst).Length))
                            {
                                File.Copy(file, dst, true);
                            }
                        }
                    }
                }
            }
            catch { }
        }

        private bool ResolveForgeVersion(string mcDir, out string jsonPath, out string verDir)
        {
            jsonPath = null;
            verDir = null;

            string versionsDir = System.IO.Path.Combine(mcDir, "versions");
            if (!Directory.Exists(versionsDir)) return false;

            // 1. Kiểm tra các tên thư mục phổ biến nhất
            string[] preferred = {
                "1.12.2-forge-14.23.5.2860",
                "Forge 1.12.2",
                "ForgeOptiFine 1.12.2",
                "1.12.2-forge1.12.2-14.23.5.2860"
            };

            foreach (string p in preferred)
            {
                string d = System.IO.Path.Combine(versionsDir, p);
                if (Directory.Exists(d))
                {
                    string j = System.IO.Path.Combine(d, p + ".json");
                    if (File.Exists(j)) { jsonPath = j; verDir = d; return true; }

                    foreach (string jFile in Directory.GetFiles(d, "*.json"))
                    {
                        jsonPath = jFile; verDir = d; return true;
                    }
                }
            }

            // 2. Quét toàn bộ thư mục versions tìm thư mục chứa cả "1.12.2" và "forge"
            try
            {
                foreach (string d in Directory.GetDirectories(versionsDir))
                {
                    string fn = System.IO.Path.GetFileName(d).ToLowerInvariant();
                    if (fn.Contains("1.12.2") && fn.Contains("forge"))
                    {
                        foreach (string j in Directory.GetFiles(d, "*.json"))
                        {
                            jsonPath = j; verDir = d; return true;
                        }
                    }
                }
            }
            catch { }

            return false;
        }

        private bool ResolveOptiFineVersion(string mcDir, out string jsonPath, out string verDir)
        {
            jsonPath = null;
            verDir = null;

            string versionsDir = System.IO.Path.Combine(mcDir, "versions");

            // 1. Kiểm tra các tên thư mục phổ biến nhất
            string[] preferred = {
                "1.12.2-OptiFine_HD_U_G5",
                "OptiFine 1.12.2",
                "OptiFine 1.12.2 - CasxDuois Model",
                "ForgeOptiFine 1.12.2"
            };

            foreach (string p in preferred)
            {
                string d = System.IO.Path.Combine(versionsDir, p);
                if (Directory.Exists(d))
                {
                    string j = System.IO.Path.Combine(d, p + ".json");
                    if (File.Exists(j)) { jsonPath = j; verDir = d; return true; }
                    j = System.IO.Path.Combine(d, "OptiFine 1.12.2.json");
                    if (File.Exists(j)) { jsonPath = j; verDir = d; return true; }

                    foreach (string jFile in Directory.GetFiles(d, "*.json"))
                    {
                        jsonPath = jFile; verDir = d; return true;
                    }
                }
            }

            // 2. Quét toàn bộ thư mục versions tìm thư mục chứa "optifine" và "1.12.2"
            if (Directory.Exists(versionsDir))
            {
                try
                {
                    foreach (string d in Directory.GetDirectories(versionsDir))
                    {
                        string fn = System.IO.Path.GetFileName(d).ToLowerInvariant();
                        if (fn.Contains("1.12.2") && fn.Contains("optifine"))
                        {
                            foreach (string j in Directory.GetFiles(d, "*.json"))
                            {
                                jsonPath = j; verDir = d; return true;
                            }
                        }
                    }
                }
                catch { }
            }

            // 3. Fallback: Kiểm tra thư mục cục bộ của client
            string localOpti = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "OptiFine 1.12.2 - CasxDuois Model");
            if (!Directory.Exists(localOpti)) localOpti = @"d:\CasxDuois\OptiFine 1.12.2 - CasxDuois Model";
            if (Directory.Exists(localOpti))
            {
                string j = System.IO.Path.Combine(localOpti, "OptiFine 1.12.2.json");
                if (File.Exists(j)) { jsonPath = j; verDir = localOpti; return true; }
            }

            return false;
        }

        private string ResolveVanillaJar(string mcDir, string versionDir)
        {
            // 1. Thư mục chuẩn versions/1.12.2/1.12.2.jar
            string std = System.IO.Path.Combine(mcDir, "versions", "1.12.2", "1.12.2.jar");
            if (File.Exists(std)) return std;

            // 2. Tìm trong chính thư mục phiên bản đang chọn
            if (!string.IsNullOrEmpty(versionDir) && Directory.Exists(versionDir))
            {
                string vj = System.IO.Path.Combine(versionDir, "1.12.2.jar");
                if (File.Exists(vj)) return vj;

                string namedJar = System.IO.Path.Combine(versionDir, System.IO.Path.GetFileName(versionDir) + ".jar");
                if (File.Exists(namedJar)) return namedJar;

                foreach (string jf in Directory.GetFiles(versionDir, "*.jar"))
                {
                    if (!jf.ToLowerInvariant().Contains("casxduois")) return jf;
                }
            }

            // 3. Quét toàn bộ versions tìm bất kỳ file 1.12.2.jar nào
            string vDir = System.IO.Path.Combine(mcDir, "versions");
            if (Directory.Exists(vDir))
            {
                try
                {
                    string[] allJars = Directory.GetFiles(vDir, "*1.12.2*.jar", SearchOption.AllDirectories);
                    if (allJars.Length > 0) return allJars[0];
                }
                catch { }
            }

            return null;
        }

        private string ResolveNatives(string mcDir, string versionDir)
        {
            // 1. Thư mục chuẩn
            string std = System.IO.Path.Combine(mcDir, "versions", "1.12.2", "1.12.2-natives");
            if (Directory.Exists(std) && Directory.GetFiles(std, "*.dll").Length > 0) return std;

            // 2. Tìm trong thư mục version
            if (!string.IsNullOrEmpty(versionDir) && Directory.Exists(versionDir))
            {
                string vn = System.IO.Path.Combine(versionDir, System.IO.Path.GetFileName(versionDir) + "-natives");
                if (Directory.Exists(vn) && Directory.GetFiles(vn, "*.dll").Length > 0) return vn;

                string subNatives = System.IO.Path.Combine(versionDir, "natives");
                if (Directory.Exists(subNatives) && Directory.GetFiles(subNatives, "*.dll").Length > 0) return subNatives;
            }

            // 3. Thư mục cục bộ client OptiFine Model
            string localNatives = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "OptiFine 1.12.2 - CasxDuois Model", "natives");
            if (!Directory.Exists(localNatives)) localNatives = @"d:\CasxDuois\OptiFine 1.12.2 - CasxDuois Model\natives";
            if (Directory.Exists(localNatives) && Directory.GetFiles(localNatives, "*.dll").Length > 0) return localNatives;

            // 4. Quét toàn bộ versions tìm thư mục chứa lwjgl64.dll hoặc *.dll
            string vDir = System.IO.Path.Combine(mcDir, "versions");
            if (Directory.Exists(vDir))
            {
                try
                {
                    foreach (string d in Directory.GetDirectories(vDir, "*natives*", SearchOption.AllDirectories))
                    {
                        if (Directory.GetFiles(d, "*.dll").Length > 0) return d;
                    }
                }
                catch { }
            }

            return std;
        }

        private void AddLibrariesFromJson(string jsonPath, string libBase, Action<string> addLib, string fallbackDir)
        {
            if (string.IsNullOrEmpty(jsonPath) || !File.Exists(jsonPath)) return;
            try
            {
                string content = File.ReadAllText(jsonPath);

                // 1. Mojang download artifact path format: "path": "com/mojang/patchy/1.3.9/patchy-1.3.9.jar"
                foreach (Match m in Regex.Matches(content, "\"path\":\\s*\"([^\"]+)\""))
                {
                    string rel = m.Groups[1].Value.Replace('/', '\\');
                    string p = System.IO.Path.Combine(libBase, rel);
                    if (File.Exists(p))
                    {
                        addLib(p);
                    }
                    else if (!string.IsNullOrEmpty(fallbackDir))
                    {
                        string fb = System.IO.Path.Combine(fallbackDir, rel);
                        if (File.Exists(fb)) addLib(fb);
                    }
                }

                // 2. Maven coordinate format: "name": "group:artifact:version"
                foreach (Match m in Regex.Matches(content, "\"name\":\\s*\"([^\"]+)\""))
                {
                    string coord = m.Groups[1].Value;
                    string rel = MavenCoordToPath(coord);
                    if (!string.IsNullOrEmpty(rel))
                    {
                        string p = System.IO.Path.Combine(libBase, rel);
                        if (File.Exists(p))
                        {
                            addLib(p);
                        }
                        else if (!string.IsNullOrEmpty(fallbackDir))
                        {
                            string fb = System.IO.Path.Combine(fallbackDir, rel);
                            if (File.Exists(fb)) addLib(fb);
                        }
                    }
                }
            }
            catch { }
        }

        private static string MavenCoordToPath(string coord)
        {
            if (string.IsNullOrEmpty(coord)) return null;
            string[] parts = coord.Split(':');
            if (parts.Length < 3) return null;
            string group = parts[0].Replace('.', '\\');
            string artifact = parts[1];
            string version = parts[2];
            string classifier = (parts.Length > 3 && !string.IsNullOrEmpty(parts[3])) ? ("-" + parts[3]) : "";
            return System.IO.Path.Combine(group, artifact, version, artifact + "-" + version + classifier + ".jar");
        }

        private Button CreateWinButton(string text, Color fg, Action onClick)
        {
            Button b = new Button
            {
                Content = text,
                Width = 36,
                Height = 30,
                Background = Brushes.Transparent,
                Foreground = new SolidColorBrush(fg),
                BorderThickness = new Thickness(0),
                FontSize = 11,
                Cursor = Cursors.Hand
            };
            b.Click += (s, e) => onClick();
            return b;
        }

        private Button CreateButton(string text, Color fg, Color bg, Action onClick)
        {
            Button b = new Button
            {
                Content = text,
                Background = new SolidColorBrush(bg),
                Foreground = new SolidColorBrush(fg),
                BorderBrush = new SolidColorBrush(Color.FromArgb(80, fg.R, fg.G, fg.B)),
                BorderThickness = new Thickness(1),
                FontSize = 11,
                FontWeight = FontWeights.Bold,
                Cursor = Cursors.Hand
            };
            b.Click += (s, e) => onClick();
            return b;
        }
    }
}
