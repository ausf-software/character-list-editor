package character_list_editor.window;

import character_list_editor.utils.ConfigManager;
import character_list_editor.utils.LocaleManager;
import character_list_editor.utils.ThemeUtil;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {
    private JComboBox<String> themeCombo;
    private JComboBox<String> localeCombo;
    private JTextField defaultCharacterNameTField;

    private JTextField defaultCharacterDirectoryTField;
    private boolean settingsChanged = false;

    public SettingsDialog(Window parent) {
        super(parent, LocaleManager.inst().getString("settings"));
        initializeUI();
        loadCurrentSettings();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(400, 250);
        setLocationRelativeTo(null);

        JPanel settingsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        settingsPanel.add(new JLabel(LocaleManager.inst().getString("settings.theme")));
        themeCombo = new JComboBox<>(new String[]{"light", "dark"});
        settingsPanel.add(themeCombo);

        settingsPanel.add(new JLabel(LocaleManager.inst().getString("settings.language")));
        localeCombo = new JComboBox<>(LocaleManager.inst().getAvailableLocales().toArray(new String[0]));
        settingsPanel.add(localeCombo);

        settingsPanel.add(new JLabel(LocaleManager.inst().getString("settings.character_name")));
        defaultCharacterNameTField = new JTextField();
        settingsPanel.add(defaultCharacterNameTField);

        settingsPanel.add(new JLabel(LocaleManager.inst().getString("settings.directory")));
        defaultCharacterDirectoryTField = new JTextField();
        settingsPanel.add(defaultCharacterDirectoryTField);

        add(settingsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton(LocaleManager.inst().getString("ok"));
        JButton cancelButton = new JButton(LocaleManager.inst().getString("cancel"));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> saveSettings());
        cancelButton.addActionListener(e -> dispose());

        themeCombo.addActionListener(e -> settingsChanged = true);
        localeCombo.addActionListener(e -> settingsChanged = true);
        defaultCharacterNameTField.addCaretListener(e -> settingsChanged = true);
    }

    private void loadCurrentSettings() {
        ConfigManager config = ConfigManager.getInstance();
        themeCombo.setSelectedItem(config.getTheme());
        localeCombo.setSelectedItem(config.getLocale());
        defaultCharacterNameTField.setText(config.getDefaultCharacterName());
        defaultCharacterDirectoryTField.setText(config.getDefaultCharacterDirectory());
        settingsChanged = false;
    }

    private void saveSettings() {
        if (settingsChanged) {
            ConfigManager config = ConfigManager.getInstance();
            config.setTheme((String) themeCombo.getSelectedItem());
            config.setLocale((String) localeCombo.getSelectedItem());
            config.setDefaultCharacterName(defaultCharacterNameTField.getText().trim());
            config.setDefaultCharacterDirectory(defaultCharacterDirectoryTField.getText());
            ThemeUtil.updateTheme();
        }
        dispose();
    }
}
