package openthedoor.ui;

import openthedoor.config.ServerConfig;
import openthedoor.scan.ApkScanner;
import openthedoor.scan.ScanResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class DesktopUi {
    private static final String[] MODES = {"apk-scan", "http-listen", "tcp-listen", "tcp-proxy", "auto"};
    private static final String APP_ICON_RESOURCE = "/openthedoor/ui/app-icon.png";
    private static final Color APP_BG = new Color(245, 247, 250);
    private static final Color PANEL_BG = Color.WHITE;
    private static final Color BORDER = new Color(218, 224, 232);
    private static final Color TEXT = new Color(24, 32, 43);
    private static final Color MUTED = new Color(92, 105, 122);
    private static final Color ORANGE = new Color(242, 126, 0);
    private static final Color TEAL = new Color(15, 118, 110);
    private static final Color SLATE = new Color(51, 65, 85);
    private static final Color DANGER = new Color(174, 56, 56);
    private static final Color CONSOLE_BG = new Color(18, 24, 33);
    private static final Color CONSOLE_FG = new Color(218, 226, 238);

    private final String configPath;
    private JFrame frame;
    private JComboBox<String> modeField;
    private JTextField hostField;
    private JTextField portField;
    private JTextField targetHostField;
    private JTextField targetPortField;
    private JTextField mockDirField;
    private JTextField httpStatusField;
    private JTextField contentTypeField;
    private JTextField logDirField;
    private JCheckBox savePacketsField;
    private JTextField maxPrintableBytesField;
    private JTextField apkPathField;
    private JTextField scanOutputField;
    private JTextArea reportArea;
    private JTextArea consoleArea;
    private JCheckBox showReportField;
    private JComboBox<String> themeField;
    private JPanel reportPanel;
    private JPanel consolePanel;
    private JPanel outputPanel;
    private JSplitPane outputSplit;
    private JButton clearReportButton;
    private JButton clearConsoleButton;
    private JButton saveButton;
    private JButton reloadButton;
    private JButton scanButton;
    private JButton startButton;
    private JButton stopButton;
    private JButton refreshReportButton;
    private JLabel statusLabel;
    private Process runningProcess;
    private ThemePalette theme = ThemePalette.classic();

    public DesktopUi(String configPath) {
        this.configPath = configPath;
    }

    public static void launch(String configPath) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Keep Swing's default look and feel.
            }
            new DesktopUi(configPath).show();
        });
    }

    private void show() {
        frame = new JFrame("OpenTheDoor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1060, 700));
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(APP_BG);
        frame.setJMenuBar(buildMenuBar());
        ImageIcon appIcon = loadAppIcon();
        if (appIcon != null) applyAppIcon(appIcon);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(14, 20, 12, 20)
        ));
        JLabel title = new JLabel("OpenTheDoor");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("Prototype servers, inspect APK endpoints, and prepare listener commands.");
        subtitle.setForeground(MUTED);
        JPanel titleBlock = new JPanel(new BorderLayout());
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(MUTED);
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        if (appIcon != null) {
            Image scaled = appIcon.getImage().getScaledInstance(38, 38, Image.SCALE_SMOOTH);
            brand.add(new JLabel(new ImageIcon(scaled)), BorderLayout.WEST);
        }
        brand.add(titleBlock, BorderLayout.CENTER);
        header.add(brand, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        frame.add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildConfigPanel(), buildWorkPanel());
        split.setResizeWeight(0.36);
        split.setBorder(null);
        frame.add(split, BorderLayout.CENTER);
        frame.add(buildFooter(), BorderLayout.SOUTH);

        loadConfig();
        loadReport();
        applyTheme("Classic Windows");

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem openReport = new JMenuItem("Open report file");
        openReport.addActionListener(e -> openReportFile());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> frame.dispose());
        file.add(openReport);
        file.addSeparator();
        file.add(exit);

        JMenu commands = new JMenu("Commands");
        JMenuItem showCommand = new JMenuItem("Show current command...");
        showCommand.addActionListener(e -> showCommandDialog());
        commands.add(showCommand);

        JMenu about = new JMenu("About");
        JMenuItem aboutItem = new JMenuItem("About OpenTheDoor");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(frame,
                "OpenTheDoor\nPrototype replacement servers and inspect legacy game clients.\n\nDeveloped by Black Pearl.",
                "About OpenTheDoor",
                JOptionPane.INFORMATION_MESSAGE));
        about.add(aboutItem);

        menuBar.add(file);
        menuBar.add(commands);
        menuBar.add(about);
        return menuBar;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(6, 12, 6, 12)
        ));
        JLabel hint = new JLabel("Ready");
        hint.setForeground(MUTED);

        JPanel right = new JPanel(new BorderLayout(8, 0));
        JLabel themeLabel = new JLabel("Theme");
        themeField = new JComboBox<>(new String[]{"Classic Windows", "Dark"});
        themeField.addActionListener(e -> applyTheme(String.valueOf(themeField.getSelectedItem())));
        right.add(themeLabel, BorderLayout.WEST);
        right.add(themeField, BorderLayout.CENTER);

        footer.add(hint, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private JPanel buildConfigPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(APP_BG);
        outer.setBorder(new EmptyBorder(16, 18, 18, 9));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL_BG);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 16, 16, 16)
        ));

        modeField = new JComboBox<>(MODES);
        hostField = new JTextField();
        portField = new JTextField();
        targetHostField = new JTextField();
        targetPortField = new JTextField();
        mockDirField = new JTextField();
        httpStatusField = new JTextField();
        contentTypeField = new JTextField();
        logDirField = new JTextField();
        savePacketsField = new JCheckBox("Save packet files");
        maxPrintableBytesField = new JTextField();
        apkPathField = new JTextField();
        scanOutputField = new JTextField();
        styleFormControls();

        int row = 0;
        row = addField(form, row, "Mode", modeField);
        row = addField(form, row, "Host", hostField);
        row = addField(form, row, "Port", portField);
        row = addField(form, row, "Target host", targetHostField);
        row = addField(form, row, "Target port", targetPortField);
        row = addFileField(form, row, "APK path", apkPathField, true);
        row = addFileField(form, row, "Scan output", scanOutputField, false);
        row = addField(form, row, "Mock directory", mockDirField);
        row = addField(form, row, "HTTP status", httpStatusField);
        row = addField(form, row, "Content type", contentTypeField);
        row = addField(form, row, "Log directory", logDirField);
        row = addField(form, row, "Max printable bytes", maxPrintableBytesField);
        addCheck(form, row++, savePacketsField);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(12, 0, 0, 0));
        saveButton = new JButton("Save config");
        reloadButton = new JButton("Reload");
        styleButton(saveButton, TEAL, Color.WHITE);
        styleButton(reloadButton, SLATE, Color.WHITE);
        saveButton.addActionListener(e -> saveConfig());
        reloadButton.addActionListener(e -> {
            loadConfig();
            loadReport();
        });
        addAction(actions, saveButton, 0);
        addAction(actions, reloadButton, 1);

        GridBagConstraints actionGbc = new GridBagConstraints();
        actionGbc.gridx = 0;
        actionGbc.gridy = row;
        actionGbc.gridwidth = 2;
        actionGbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(actions, actionGbc);

        outer.add(form, BorderLayout.NORTH);
        return outer;
    }

    private ImageIcon loadAppIcon() {
        java.net.URL resource = DesktopUi.class.getResource(APP_ICON_RESOURCE);
        return resource == null ? null : new ImageIcon(resource);
    }

    private void applyAppIcon(ImageIcon appIcon) {
        Image base = appIcon.getImage();
        frame.setIconImage(base);
        frame.setIconImages(iconSizes(base));
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(base);
                }
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Some launchers/JREs do not expose taskbar icon control.
        }
    }

    private List<Image> iconSizes(Image base) {
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        List<Image> images = new ArrayList<>();
        for (int size : sizes) {
            images.add(base.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        }
        images.add(base);
        return images;
    }

    private JPanel buildWorkPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 12));
        outer.setBackground(APP_BG);
        outer.setBorder(new EmptyBorder(16, 9, 18, 18));

        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setBackground(PANEL_BG);
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        scanButton = new JButton("Run APK scan");
        startButton = new JButton("Start selected mode");
        stopButton = new JButton("Stop");
        refreshReportButton = new JButton("Refresh report");
        styleButton(scanButton, ORANGE, TEXT);
        styleButton(startButton, TEAL, TEXT);
        styleButton(stopButton, DANGER, TEXT);
        styleButton(refreshReportButton, SLATE, TEXT);
        scanButton.addActionListener(e -> runScan());
        startButton.addActionListener(e -> startSelectedMode());
        stopButton.addActionListener(e -> stopSelectedMode());
        refreshReportButton.addActionListener(e -> loadReport());
        addAction(buttons, scanButton, 0);
        addAction(buttons, startButton, 1);
        addAction(buttons, stopButton, 2);
        addAction(buttons, refreshReportButton, 3);

        showReportField = new JCheckBox("Show APK scan report");
        showReportField.setOpaque(false);
        showReportField.setForeground(TEXT);
        showReportField.addActionListener(e -> updateOutputVisibility());
        JPanel actionOptions = new JPanel(new BorderLayout());
        actionOptions.setOpaque(false);
        actionOptions.setBorder(new EmptyBorder(10, 0, 0, 0));
        actionOptions.add(showReportField, BorderLayout.WEST);

        actionPanel.add(buttons, BorderLayout.CENTER);
        actionPanel.add(actionOptions, BorderLayout.SOUTH);
        clearReportButton = iconButton("Clear APK scan report");
        clearReportButton.addActionListener(e -> {
            reportArea.setText("");
            updateClearButtons();
            status("APK scan report view cleared.");
        });
        clearConsoleButton = iconButton("Clear runtime output");
        clearConsoleButton.addActionListener(e -> {
            consoleArea.setText("");
            updateClearButtons();
            status("Runtime output cleared.");
        });

        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        reportArea.setMargin(new Insets(12, 12, 12, 12));
        reportArea.setBackground(Color.WHITE);
        reportArea.setForeground(TEXT);
        reportArea.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateClearButtons));
        JScrollPane reportScroll = new JScrollPane(reportArea);
        reportScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        reportPanel = outputSection("APK scan report", null, clearReportButton, reportScroll);

        consoleArea = new JTextArea(8, 30);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleArea.setLineWrap(false);
        consoleArea.setMargin(new Insets(12, 12, 12, 12));
        consoleArea.setBackground(CONSOLE_BG);
        consoleArea.setForeground(CONSOLE_FG);
        consoleArea.setCaretColor(CONSOLE_FG);
        consoleArea.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateClearButtons));
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 72)));
        consolePanel = outputSection("Runtime output", null, clearConsoleButton, consoleScroll);

        outputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, reportPanel, consolePanel);
        outputSplit.setResizeWeight(0.72);
        outputSplit.setBorder(null);
        outputSplit.setDividerSize(7);
        outputPanel = new JPanel(new BorderLayout());
        outputPanel.setOpaque(false);

        outer.add(actionPanel, BorderLayout.NORTH);
        outer.add(outputPanel, BorderLayout.CENTER);
        updateOutputVisibility();
        updateClearButtons();
        return outer;
    }

    private JPanel outputSection(String title, JCheckBox toggle, JButton clearButton, JScrollPane content) {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(PANEL_BG);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        header.add(label, BorderLayout.WEST);

        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.setOpaque(false);
        if (toggle != null) tools.add(toggle, BorderLayout.WEST);
        tools.add(clearButton, BorderLayout.EAST);
        header.add(tools, BorderLayout.EAST);

        section.add(header, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    private void updateOutputVisibility() {
        if (outputPanel == null) return;
        outputPanel.removeAll();
        boolean showReport = showReportField != null && showReportField.isSelected();
        if (showReport) {
            outputSplit.setTopComponent(reportPanel);
            outputSplit.setBottomComponent(consolePanel);
            outputPanel.add(outputSplit, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> outputSplit.setDividerLocation(0.46));
        } else {
            outputPanel.add(consolePanel, BorderLayout.CENTER);
        }
        applyThemeTo(outputPanel);
        if (reportArea != null) {
            reportArea.setBackground(theme.reportBg);
            reportArea.setForeground(theme.text);
            reportArea.setCaretColor(theme.text);
        }
        if (consoleArea != null) {
            consoleArea.setBackground(theme.consoleBg);
            consoleArea.setForeground(theme.consoleFg);
            consoleArea.setCaretColor(theme.consoleFg);
        }
        outputPanel.revalidate();
        outputPanel.repaint();
    }

    private void updateClearButtons() {
        if (clearReportButton != null && reportArea != null) {
            clearReportButton.setEnabled(reportArea.getDocument().getLength() > 0);
        }
        if (clearConsoleButton != null && consoleArea != null) {
            clearConsoleButton.setEnabled(consoleArea.getDocument().getLength() > 0);
        }
    }

    private JButton iconButton(String tooltip) {
        JButton button = new JButton(new TrashIcon());
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(34, 30));
        button.setMinimumSize(new Dimension(34, 30));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(BORDER));
        button.setBackground(new Color(252, 253, 255));
        button.setForeground(DANGER);
        return button;
    }

    private void styleFormControls() {
        styleField(hostField);
        styleField(portField);
        styleField(targetHostField);
        styleField(targetPortField);
        styleField(mockDirField);
        styleField(httpStatusField);
        styleField(contentTypeField);
        styleField(logDirField);
        styleField(maxPrintableBytesField);
        styleField(apkPathField);
        styleField(scanOutputField);
        modeField.setBackground(Color.WHITE);
        modeField.setForeground(TEXT);
        modeField.setBorder(BorderFactory.createLineBorder(new Color(188, 198, 211)));
        savePacketsField.setOpaque(false);
        savePacketsField.setForeground(TEXT);
    }

    private void styleField(JTextField field) {
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(188, 198, 211)),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setFocusPainted(false);
        button.putClientProperty("accent", background);
        button.setOpaque(true);
        restyleButton(button);
    }

    private void restyleButton(JButton button) {
        Color accent = (Color) button.getClientProperty("accent");
        if (accent == null) accent = theme.border;
        Color background = theme.dark ? darkButtonColor(accent) : theme.buttonBg;
        Color foreground = theme.dark ? Color.WHITE : theme.text;
        Color border = theme.dark ? background.brighter() : theme.buttonBorder;
        button.setUI(new BasicButtonUI());
        button.setBackground(background);
        button.setForeground(foreground);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(7, 12, 7, 12)
        ));
    }

    private Color darkButtonColor(Color accent) {
        if (accent.equals(ORANGE)) return new Color(194, 92, 0);
        if (accent.equals(TEAL)) return new Color(12, 105, 98);
        if (accent.equals(DANGER)) return new Color(143, 48, 56);
        return new Color(55, 65, 81);
    }

    private void applyTheme(String name) {
        theme = "Dark".equals(name) ? ThemePalette.dark() : ThemePalette.classic();
        if (frame == null) return;
        UIManager.put("MenuBar.background", theme.menuBg);
        UIManager.put("Menu.background", theme.menuBg);
        UIManager.put("Menu.foreground", theme.menuText);
        UIManager.put("MenuItem.background", theme.menuBg);
        UIManager.put("MenuItem.foreground", theme.menuText);
        UIManager.put("ComboBox.background", theme.fieldBg);
        UIManager.put("ComboBox.foreground", theme.text);
        UIManager.put("ComboBox.selectionBackground", theme.selectionBg);
        UIManager.put("ComboBox.selectionForeground", theme.selectionText);
        frame.getContentPane().setBackground(theme.appBg);
        if (frame.getJMenuBar() != null) styleMenuBar(frame.getJMenuBar());
        applyThemeTo(frame.getContentPane());
        if (statusLabel != null) statusLabel.setForeground(theme.muted);
        if (reportArea != null) {
            reportArea.setBackground(theme.reportBg);
            reportArea.setForeground(theme.text);
            reportArea.setCaretColor(theme.text);
        }
        if (consoleArea != null) {
            consoleArea.setBackground(theme.consoleBg);
            consoleArea.setForeground(theme.consoleFg);
            consoleArea.setCaretColor(theme.consoleFg);
        }
        if (outputSplit != null) outputSplit.setBackground(theme.appBg);
        frame.repaint();
    }

    private void styleMenuBar(JMenuBar menuBar) {
        menuBar.setOpaque(true);
        menuBar.setBackground(theme.menuBg);
        menuBar.setForeground(theme.menuText);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border));
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            styleMenu(menuBar.getMenu(i));
        }
    }

    private void styleMenu(JMenu menu) {
        if (menu == null) return;
        menu.setOpaque(true);
        menu.setBackground(theme.menuBg);
        menu.setForeground(theme.menuText);
        menu.getPopupMenu().setBackground(theme.menuBg);
        menu.getPopupMenu().setBorder(BorderFactory.createLineBorder(theme.border));
        for (Component child : menu.getMenuComponents()) {
            if (child instanceof JMenu) {
                styleMenu((JMenu) child);
            } else if (child instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) child;
                item.setOpaque(true);
                item.setBackground(theme.menuBg);
                item.setForeground(theme.menuText);
            }
        }
    }

    private void applyThemeTo(Component component) {
        if (component instanceof JPanel) {
            component.setBackground(theme.panelBg);
            JPanel panel = (JPanel) component;
            if (panel.getBorder() != null) {
                panel.setBorder(retintBorder(panel.getBorder(), panel));
            }
        } else if (component instanceof JLabel) {
            component.setForeground(theme.text);
        } else if (component instanceof JTextField) {
            JTextField field = (JTextField) component;
            field.setBackground(theme.fieldBg);
            field.setForeground(theme.text);
            field.setCaretColor(theme.text);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.fieldBorder),
                    new EmptyBorder(4, 8, 4, 8)
            ));
        } else if (component instanceof JTextArea) {
            JTextArea area = (JTextArea) component;
            area.setBackground(theme.fieldBg);
            area.setForeground(theme.text);
            area.setCaretColor(theme.text);
        } else if (component instanceof JComboBox) {
            restyleComboBox((JComboBox<?>) component);
        } else if (component instanceof JCheckBox) {
            component.setBackground(theme.panelBg);
            component.setForeground(theme.text);
        } else if (component instanceof JButton) {
            restyleButton((JButton) component);
        } else if (component instanceof JMenuBar || component instanceof JMenu || component instanceof JMenuItem) {
            component.setBackground(theme.panelBg);
            component.setForeground(theme.text);
        } else if (component instanceof JScrollPane) {
            component.setBackground(theme.panelBg);
            ((JScrollPane) component).getViewport().setBackground(theme.fieldBg);
            ((JScrollPane) component).setBorder(BorderFactory.createLineBorder(theme.border));
        } else {
            component.setBackground(theme.panelBg);
            component.setForeground(theme.text);
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyThemeTo(child);
            }
        }
    }

    private void restyleComboBox(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("v");
                arrow.setUI(new BasicButtonUI());
                arrow.setFocusPainted(false);
                arrow.setContentAreaFilled(true);
                arrow.setBorderPainted(false);
                arrow.setOpaque(true);
                arrow.setBackground(theme.fieldBg);
                arrow.setForeground(theme.text);
                return arrow;
            }
        });
        combo.setOpaque(true);
        combo.setBackground(theme.fieldBg);
        combo.setForeground(theme.text);
        combo.setBorder(BorderFactory.createLineBorder(theme.fieldBorder));
        combo.setFocusable(true);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(3, 8, 3, 8));
                label.setBackground(isSelected ? theme.selectionBg : theme.fieldBg);
                label.setForeground(isSelected ? theme.selectionText : theme.text);
                list.setBackground(theme.fieldBg);
                list.setForeground(theme.text);
                list.setSelectionBackground(theme.selectionBg);
                list.setSelectionForeground(theme.selectionText);
                return label;
            }
        });
    }

    private Border retintBorder(Border border, Component owner) {
        if (border instanceof EmptyBorder) {
            return border;
        }
        if (border instanceof CompoundBorder) {
            CompoundBorder compound = (CompoundBorder) border;
            return BorderFactory.createCompoundBorder(
                    retintBorder(compound.getOutsideBorder(), owner),
                    retintBorder(compound.getInsideBorder(), owner)
            );
        }
        Insets insets = border.getBorderInsets(owner);
        return BorderFactory.createMatteBorder(insets.top, insets.left, insets.bottom, insets.right, theme.border);
    }

    private int addField(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(5, 0, 5, 8);
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(TEXT);
        panel.add(labelComponent, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(5, 0, 5, 0);
        panel.add(field, right);
        return row + 1;
    }

    private int addFileField(JPanel panel, int row, String label, JTextField field, boolean apk) {
        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setOpaque(false);
        wrapper.add(field, BorderLayout.CENTER);
        JButton browse = new JButton("...");
        styleButton(browse, new Color(232, 238, 246), TEXT);
        browse.addActionListener(e -> chooseFile(field, apk));
        wrapper.add(browse, BorderLayout.EAST);
        return addField(panel, row, label, wrapper);
    }

    private void addCheck(JPanel panel, int row, JCheckBox box) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(box, gbc);
    }

    private void addAction(JPanel panel, JButton button, int column) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = column;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, column == 0 ? 0 : 8, 0, 0);
        panel.add(button, gbc);
    }

    private void chooseFile(JTextField field, boolean apk) {
        JFileChooser chooser = new JFileChooser(new File("."));
        if (apk) chooser.setFileFilter(new FileNameExtensionFilter("Android APK", "apk"));
        int result = apk ? chooser.showOpenDialog(frame) : chooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getPath());
        }
    }

    private void loadConfig() {
        ServerConfig config = ServerConfig.load(configPath);
        modeField.setSelectedItem(config.getMode());
        hostField.setText(config.getHost());
        portField.setText(String.valueOf(config.getPort()));
        targetHostField.setText(config.getTargetHost());
        targetPortField.setText(String.valueOf(config.getTargetPort()));
        mockDirField.setText(config.getMockDir());
        httpStatusField.setText(String.valueOf(config.getDefaultHttpStatus()));
        contentTypeField.setText(config.getDefaultHttpContentType());
        logDirField.setText(config.getLogDir());
        savePacketsField.setSelected(config.isSavePackets());
        maxPrintableBytesField.setText(String.valueOf(config.getMaxPrintableBytes()));
        apkPathField.setText(config.getApkPath());
        scanOutputField.setText(config.getScanOutput());
        status("Configuration loaded.");
    }

    private void saveConfig() {
        try {
            Properties properties = new Properties();
            File file = new File(configPath);
            if (file.isFile()) {
                try (FileInputStream input = new FileInputStream(file)) {
                    properties.load(input);
                }
            }

            properties.setProperty("mode", value(modeField));
            properties.setProperty("host", hostField.getText().trim());
            properties.setProperty("port", portField.getText().trim());
            properties.setProperty("targetHost", targetHostField.getText().trim());
            properties.setProperty("targetPort", targetPortField.getText().trim());
            properties.setProperty("mockDir", mockDirField.getText().trim());
            properties.setProperty("defaultHttpStatus", httpStatusField.getText().trim());
            properties.setProperty("defaultHttpContentType", contentTypeField.getText().trim());
            properties.setProperty("logDir", logDirField.getText().trim());
            properties.setProperty("savePackets", String.valueOf(savePacketsField.isSelected()));
            properties.setProperty("maxPrintableBytes", maxPrintableBytesField.getText().trim());
            properties.setProperty("apkPath", apkPathField.getText().trim());
            properties.setProperty("scanOutput", scanOutputField.getText().trim());

            try (FileOutputStream output = new FileOutputStream(file)) {
                properties.store(output, "OpenTheDoor configuration");
            }
            status("Configuration saved.");
        } catch (IOException e) {
            error("Could not save configuration", e);
        }
    }

    private void runScan() {
        saveConfig();
        status("APK scan running...");
        reportArea.setText("Scanning " + apkPathField.getText().trim() + "...\n");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                ServerConfig config = ServerConfig.load(configPath);
                ScanResult result = new ApkScanner(config.getApkPath()).scan();
                result.writeMarkdown(config.getScanOutput());
                return result.toMarkdown();
            }

            @Override
            protected void done() {
                try {
                    reportArea.setText(get());
                    reportArea.setCaretPosition(0);
                    status("APK scan complete.");
                } catch (Exception e) {
                    error("APK scan failed", e);
                }
            }
        }.execute();
    }

    private void loadReport() {
        try {
            File report = new File(scanOutputField != null ? scanOutputField.getText().trim() : ServerConfig.load(configPath).getScanOutput());
            if (!report.isFile()) {
                reportArea.setText("No report found yet.");
                return;
            }
            reportArea.setText(new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8));
            reportArea.setCaretPosition(0);
            status("Report loaded.");
        } catch (Exception e) {
            error("Could not load report", e);
        }
    }

    private void openReportFile() {
        try {
            File report = new File(scanOutputField.getText().trim());
            if (!report.isFile()) {
                JOptionPane.showMessageDialog(frame, "No report file found yet.", "Open report", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(report);
        } catch (Exception e) {
            error("Could not open report", e);
        }
    }

    private void startSelectedMode() {
        if (runningProcess != null && runningProcess.isAlive()) {
            JOptionPane.showMessageDialog(frame, "A process is already running.", "Start mode", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        saveConfig();
        consoleArea.setText("");

        try {
            String javaBin = new File(new File(System.getProperty("java.home"), "bin"), isWindows() ? "java.exe" : "java").getPath();
            ProcessBuilder builder = new ProcessBuilder(Arrays.asList(
                    javaBin,
                    "-cp",
                    "OpenTheDoor.jar" + File.pathSeparator + "libs" + File.separator + "netty-all-4.1.68.Final.jar",
                    "openthedoor.Main",
                    "--cli",
                    configPath
            ));
            builder.directory(new File("."));
            runningProcess = builder.start();
            status("Started " + value(modeField) + ".");
            appendConsole("$ " + String.join(" ", builder.command()) + "\n");
            pipe(runningProcess.getInputStream());
            pipe(runningProcess.getErrorStream());
            new Thread(() -> {
                try {
                    int code = runningProcess.waitFor();
                    SwingUtilities.invokeLater(() -> status("Process exited with code " + code + "."));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }, "openthedoor-process-wait").start();
        } catch (IOException e) {
            error("Could not start selected mode", e);
        }
    }

    private void stopSelectedMode() {
        if (runningProcess == null || !runningProcess.isAlive()) {
            status("No running process.");
            return;
        }
        runningProcess.destroy();
        status("Stopping process...");
    }

    private void pipe(InputStream input) {
        new Thread(() -> {
            byte[] buffer = new byte[4096];
            int n;
            try {
                while ((n = input.read(buffer)) != -1) {
                    String text = new String(buffer, 0, n, StandardCharsets.UTF_8);
                    SwingUtilities.invokeLater(() -> appendConsole(text));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> appendConsole("\n[UI] Output stream closed: " + e.getMessage() + "\n"));
            }
        }, "openthedoor-process-output").start();
    }

    private void appendConsole(String text) {
        consoleArea.append(text);
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
    }

    private String currentCommand() {
        String cp = "OpenTheDoor.jar" + File.pathSeparator + "libs" + File.separator + "netty-all-4.1.68.Final.jar";
        String mode = value(modeField);
        String command = "java -cp \"" + cp + "\" openthedoor.Main --cli " + configPath;
        if ("apk-scan".equals(mode)) {
            command += "\n\nThis runs the APK scan and writes: " + scanOutputField.getText().trim();
        } else if ("tcp-proxy".equals(mode)) {
            command += "\n\nProxy target: " + targetHostField.getText().trim() + ":" + targetPortField.getText().trim();
        } else {
            command += "\n\nListener: " + hostField.getText().trim() + ":" + portField.getText().trim();
        }
        return command;
    }

    private void showCommandDialog() {
        JTextArea commandText = new JTextArea(currentCommand(), 7, 72);
        commandText.setEditable(false);
        commandText.setLineWrap(true);
        commandText.setWrapStyleWord(true);
        commandText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commandText.setMargin(new Insets(10, 10, 10, 10));
        commandText.setBackground(theme.fieldBg);
        commandText.setForeground(theme.text);

        JButton copyButton = new JButton("Copy command");
        styleButton(copyButton, TEAL, theme.text);
        copyButton.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(commandText.getText()), null);
            status("Command copied.");
        });

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setBackground(theme.panelBg);
        panel.add(new JScrollPane(commandText), BorderLayout.CENTER);
        panel.add(copyButton, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(frame, panel, "Current command", JOptionPane.PLAIN_MESSAGE);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String value(JComboBox<String> combo) {
        Object selected = combo.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void status(String message) {
        if (statusLabel != null) statusLabel.setText(message);
    }

    private void error(String title, Exception e) {
        status(title);
        String message = e.getMessage() == null ? e.toString() : e.getMessage();
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static final class ThemePalette {
        final Color appBg;
        final Color panelBg;
        final Color fieldBg;
        final Color fieldBorder;
        final Color buttonBg;
        final Color buttonBorder;
        final Color border;
        final Color text;
        final Color muted;
        final Color menuBg;
        final Color menuText;
        final Color selectionBg;
        final Color selectionText;
        final Color reportBg;
        final Color consoleBg;
        final Color consoleFg;
        final boolean dark;

        private ThemePalette(Color appBg, Color panelBg, Color fieldBg, Color fieldBorder,
                             Color buttonBg, Color buttonBorder, Color border, Color text, Color muted,
                             Color menuBg, Color menuText, Color selectionBg, Color selectionText,
                             Color reportBg, Color consoleBg, Color consoleFg, boolean dark) {
            this.appBg = appBg;
            this.panelBg = panelBg;
            this.fieldBg = fieldBg;
            this.fieldBorder = fieldBorder;
            this.buttonBg = buttonBg;
            this.buttonBorder = buttonBorder;
            this.border = border;
            this.text = text;
            this.muted = muted;
            this.menuBg = menuBg;
            this.menuText = menuText;
            this.selectionBg = selectionBg;
            this.selectionText = selectionText;
            this.reportBg = reportBg;
            this.consoleBg = consoleBg;
            this.consoleFg = consoleFg;
            this.dark = dark;
        }

        static ThemePalette classic() {
            return new ThemePalette(
                    new Color(245, 247, 250),
                    Color.WHITE,
                    Color.WHITE,
                    new Color(188, 198, 211),
                    new Color(238, 241, 245),
                    new Color(158, 169, 184),
                    new Color(218, 224, 232),
                    TEXT,
                    MUTED,
                    Color.WHITE,
                    TEXT,
                    new Color(0, 120, 215),
                    Color.WHITE,
                    Color.WHITE,
                    Color.WHITE,
                    TEXT,
                    false
            );
        }

        static ThemePalette dark() {
            return new ThemePalette(
                    new Color(17, 24, 39),
                    new Color(24, 32, 44),
                    new Color(31, 41, 55),
                    new Color(75, 85, 99),
                    new Color(38, 49, 65),
                    new Color(92, 108, 130),
                    new Color(55, 65, 81),
                    new Color(229, 235, 243),
                    new Color(168, 180, 196),
                    new Color(17, 24, 39),
                    new Color(229, 235, 243),
                    new Color(15, 118, 110),
                    Color.WHITE,
                    new Color(19, 27, 38),
                    new Color(10, 15, 23),
                    new Color(220, 230, 242),
                    true
            );
        }
    }

    private static final class SimpleDocumentListener implements DocumentListener {
        private final Runnable callback;

        SimpleDocumentListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            callback.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            callback.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            callback.run();
        }
    }

    private static final class TrashIcon implements Icon {
        @Override
        public int getIconWidth() {
            return 15;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.isEnabled() ? DANGER : new Color(150, 160, 174));
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 4, x + 12, y + 4);
            g2.drawLine(x + 6, y + 2, x + 9, y + 2);
            g2.drawRoundRect(x + 4, y + 5, 7, 9, 2, 2);
            g2.drawLine(x + 6, y + 7, x + 6, y + 12);
            g2.drawLine(x + 9, y + 7, x + 9, y + 12);
            g2.dispose();
        }
    }
}
