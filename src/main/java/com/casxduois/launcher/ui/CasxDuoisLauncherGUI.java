package com.casxduois.launcher.ui;

import com.casxduois.launcher.manager.InstanceManager;
import com.casxduois.launcher.manager.LauncherAccountManager;
import com.casxduois.launcher.model.InstanceConfig;
import com.casxduois.launcher.model.LauncherAccount;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Giao diện Desktop Dark Theme của CasxDuois Launcher (Lite như Prism - Dễ như Legacy)
 */
public class CasxDuoisLauncherGUI extends JFrame {

    private final LauncherAccountManager accountManager;
    private final InstanceManager instanceManager;
    private final InstanceConfig config;

    private JTextArea logArea;
    private JSlider ramSlider;
    private JLabel ramLabel;
    private DefaultTableModel accountTableModel;
    private JPanel accountsCheckPanel;

    public CasxDuoisLauncherGUI() {
        File baseDir = new File(System.getProperty("user.home"), ".casxduois_launcher");
        if (!baseDir.exists()) baseDir.mkdirs();

        this.accountManager = new LauncherAccountManager(baseDir);
        this.instanceManager = new InstanceManager();
        this.config = new InstanceConfig();

        setTitle("CasxDuois Launcher v1.0 - Trình Khởi Chạy Đa Tab Siêu Lite");
        setSize(780, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Thiết lập giao diện Dark Theme
        initDarkTheme();
        initUI();
    }

    private void initDarkTheme() {
        UIManager.put("Panel.background", new Color(8, 9, 13));
        UIManager.put("TabbedPane.background", new Color(14, 16, 24));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.selected", Color.WHITE);
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBackground(new Color(8, 9, 13));
        rootPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(14, 16, 24));
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("Minecraft Client");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel subLabel = new JLabel("Phiên bản 1.12.2");
        subLabel.setForeground(new Color(148, 163, 184));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(subLabel, BorderLayout.EAST);
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Panel
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("Khởi chạy", createMultiTabPanel());
        tabbedPane.addTab("Tài khoản", createAccountPanel());
        tabbedPane.addTab("Cài đặt", createSettingsPanel());

        rootPanel.add(tabbedPane, BorderLayout.CENTER);

        // Log Console Area
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(new Color(20, 22, 32));
        logPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        logArea = new JTextArea(4, 50);
        logArea.setEditable(false);
        logArea.setBackground(new Color(10, 12, 18));
        logArea.setForeground(new Color(0, 230, 118));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setText(">>> CasxDuois Launcher đã sẵn sàng. Hãy chọn tài khoản và bấm Mở Game!\n");

        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);
        rootPanel.add(logPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
    }

    private JPanel createMultiTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(20, 22, 32));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Danh sách tài khoản checkbox
        accountsCheckPanel = new JPanel();
        accountsCheckPanel.setLayout(new BoxLayout(accountsCheckPanel, BoxLayout.Y_AXIS));
        accountsCheckPanel.setBackground(new Color(15, 17, 26));
        refreshAccountsCheckPanel();

        JScrollPane scrollPane = new JScrollPane(accountsCheckPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(40, 43, 60)), "Chọn tài khoản", 0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Sidebar điều khiển & RAM
        JPanel controlPanel = new JPanel(new GridLayout(6, 1, 8, 8));
        controlPanel.setBackground(new Color(20, 22, 32));
        controlPanel.setPreferredSize(new Dimension(240, 0));

        ramLabel = new JLabel("Bộ nhớ RAM: " + config.getRamPerTabMB() + " MB");
        ramLabel.setForeground(Color.WHITE);
        ramLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        ramSlider = new JSlider(256, 1024, config.getRamPerTabMB());
        ramSlider.setBackground(new Color(20, 22, 32));
        ramSlider.setForeground(Color.WHITE);
        ramSlider.addChangeListener(e -> {
            config.setRamPerTabMB(ramSlider.getValue());
            ramLabel.setText("Bộ nhớ RAM: " + config.getRamPerTabMB() + " MB");
        });

        JButton btnSelectAll = createStyledButton("Chọn tất cả", new Color(40, 43, 60), Color.WHITE);
        btnSelectAll.addActionListener(e -> {
            for (LauncherAccount acc : accountManager.getAccounts()) acc.setSelected(true);
            refreshAccountsCheckPanel();
        });

        JButton btnDeselectAll = createStyledButton("Bỏ chọn", new Color(40, 43, 60), Color.WHITE);
        btnDeselectAll.addActionListener(e -> {
            for (LauncherAccount acc : accountManager.getAccounts()) acc.setSelected(false);
            refreshAccountsCheckPanel();
        });

        JButton btnLaunch = createStyledButton("Chơi ngay", Color.WHITE, new Color(8, 9, 13));
        btnLaunch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLaunch.addActionListener(e -> {
            List<LauncherAccount> selected = accountManager.getAccounts().stream().filter(LauncherAccount::isSelected).collect(Collectors.toList());
            if (selected.isEmpty()) {
                log("Vui lòng chọn ít nhất một tài khoản để mở.");
                return;
            }
            instanceManager.launchBatch(selected, config, this::log);
        });

        JButton btnKillAll = createStyledButton("Đóng game", new Color(220, 53, 69), Color.WHITE);
        btnKillAll.addActionListener(e -> instanceManager.killAll(this::log));

        controlPanel.add(ramLabel);
        controlPanel.add(ramSlider);
        controlPanel.add(btnSelectAll);
        controlPanel.add(btnDeselectAll);
        controlPanel.add(btnLaunch);
        controlPanel.add(btnKillAll);

        panel.add(controlPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(14, 16, 24));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Thanh công cụ thêm tài khoản
        JPanel topToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topToolBar.setBackground(new Color(18, 20, 30));

        JTextField nameField = new JTextField(12);
        nameField.setBackground(new Color(10, 12, 18));
        nameField.setForeground(Color.WHITE);
        nameField.setCaretColor(Color.WHITE);

        JButton btnAdd = createStyledButton("Thêm nick", Color.WHITE, new Color(8, 9, 13));
        btnAdd.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                accountManager.addAccount(name);
                nameField.setText("");
                refreshAccountTable();
                refreshAccountsCheckPanel();
                log("Đã thêm tài khoản: " + name);
            }
        });

        topToolBar.add(new JLabel("Tên nick: "));
        topToolBar.add(nameField);
        topToolBar.add(btnAdd);
        panel.add(topToolBar, BorderLayout.NORTH);

        // Bảng tài khoản
        String[] cols = {"Tên Tài Khoản", "Loại", "Trạng Thái"};
        accountTableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(accountTableModel);
        table.setBackground(new Color(15, 17, 26));
        table.setForeground(Color.WHITE);
        table.setRowHeight(24);
        refreshAccountTable();

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton btnDelete = createStyledButton("Xóa tài khoản đã chọn", new Color(220, 53, 69), Color.WHITE);
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < accountManager.getAccounts().size()) {
                LauncherAccount acc = accountManager.getAccounts().get(row);
                accountManager.removeAccount(acc);
                refreshAccountTable();
                refreshAccountsCheckPanel();
                log("Đã xóa: " + acc.getUsername());
            }
        });
        panel.add(btnDelete, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBackground(new Color(20, 22, 32));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Đường dẫn Java
        JPanel javaPanel = new JPanel(new BorderLayout(5, 5));
        javaPanel.setBackground(new Color(20, 22, 32));
        JLabel jLbl = new JLabel("Đường dẫn Java 8 (javaw.exe):");
        jLbl.setForeground(Color.WHITE);
        JTextField javaField = new JTextField(config.getJavaPath());
        javaField.setBackground(new Color(15, 17, 26));
        javaField.setForeground(Color.WHITE);
        javaPanel.add(jLbl, BorderLayout.NORTH);
        javaPanel.add(javaField, BorderLayout.CENTER);

        // Thư mục .minecraft
        JPanel mcPanel = new JPanel(new BorderLayout(5, 5));
        mcPanel.setBackground(new Color(20, 22, 32));
        JLabel mcLbl = new JLabel("Thư mục .minecraft:");
        mcLbl.setForeground(Color.WHITE);
        JTextField mcField = new JTextField(config.getMinecraftDir());
        mcField.setBackground(new Color(15, 17, 26));
        mcField.setForeground(Color.WHITE);
        mcPanel.add(mcLbl, BorderLayout.NORTH);
        mcPanel.add(mcField, BorderLayout.CENTER);

        // JVM Args
        JPanel jvmPanel = new JPanel(new BorderLayout(5, 5));
        jvmPanel.setBackground(new Color(20, 22, 32));
        JLabel jvmLbl = new JLabel("Cờ tối ưu JVM GC (Mặc định siêu nhẹ):");
        jvmLbl.setForeground(Color.WHITE);
        JTextField jvmField = new JTextField(config.getCustomJvmArgs());
        jvmField.setBackground(new Color(15, 17, 26));
        jvmField.setForeground(Color.WHITE);
        jvmPanel.add(jvmLbl, BorderLayout.NORTH);
        jvmPanel.add(jvmField, BorderLayout.CENTER);

        JButton btnSave = createStyledButton("Lưu cài đặt", new Color(0, 229, 255), Color.BLACK);
        btnSave.addActionListener(e -> {
            config.setJavaPath(javaField.getText().trim());
            config.setMinecraftDir(mcField.getText().trim());
            config.setCustomJvmArgs(jvmField.getText().trim());
            log("Đã lưu cài đặt khởi chạy.");
        });

        panel.add(javaPanel);
        panel.add(mcPanel);
        panel.add(jvmPanel);
        panel.add(btnSave);

        return panel;
    }

    private void refreshAccountsCheckPanel() {
        accountsCheckPanel.removeAll();
        for (LauncherAccount acc : accountManager.getAccounts()) {
            JCheckBox cb = new JCheckBox(acc.getUsername(), acc.isSelected());
            cb.setBackground(new Color(15, 17, 26));
            cb.setForeground(Color.WHITE);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cb.addActionListener(e -> acc.setSelected(cb.isSelected()));
            accountsCheckPanel.add(cb);
        }
        accountsCheckPanel.revalidate();
        accountsCheckPanel.repaint();
    }

    private void refreshAccountTable() {
        accountTableModel.setRowCount(0);
        for (LauncherAccount acc : accountManager.getAccounts()) {
            accountTableModel.addRow(new Object[]{acc.getUsername(), "Offline", acc.isRunning() ? "Đang chạy" : "Chưa mở"});
        }
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return btn;
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(">>> " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
