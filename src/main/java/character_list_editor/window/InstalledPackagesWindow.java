package character_list_editor.window;

import character_list_editor.component.RulesPackageList;
import character_list_editor.database.CharacterRepository;
import character_list_editor.database.Package;
import character_list_editor.utils.LocaleManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InstalledPackagesWindow extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(InstalledPackagesWindow.class);

    private final LocaleManager localeManager;
    private final CharacterRepository characterRepository;

    private JPanel mainPanel;
    private JScrollPane packagesScrollPane;
    private RulesPackageList rulesPackageList;
    private JButton addPackageButton;
    private JButton deletePackagesButton;

    private List<Package> currentPackages; // для доступа к данным при удалении

    public InstalledPackagesWindow(Window owner) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.localeManager = LocaleManager.inst();
        try {
            this.characterRepository = CharacterRepository.getInstance();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize repository", e);
        }

        initComponents();
        layoutComponents();
        setupListeners();
        refreshPackagesList();

        pack();
        setLocationRelativeTo(owner);
        setResizable(true);
        setMinimumSize(new Dimension(500, 400));
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setName("mainPanel");
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        packagesScrollPane = new JScrollPane();
        packagesScrollPane.setName("packagesScrollPane");

        addPackageButton = new JButton();
        addPackageButton.setName("addPackageButton");
        addPackageButton.setText(localeManager.getString("installedPackages.button.add"));

        deletePackagesButton = new JButton();
        deletePackagesButton.setName("deletePackagesButton");
        deletePackagesButton.setText(localeManager.getString("installedPackages.button.delete"));
        deletePackagesButton.setEnabled(false);
    }

    private void layoutComponents() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        buttonPanel.add(addPackageButton);
        buttonPanel.add(deletePackagesButton);

        mainPanel.add(packagesScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setTitle(localeManager.getString("installedPackages.title"));
    }

    private void setupListeners() {
        addPackageButton.addActionListener(this::onAddPackage);
        deletePackagesButton.addActionListener(this::onDeletePackages);
    }

    /**
     * Обновляет список пакетов из репозитория.
     */
    public void refreshPackagesList() {
        try {
            currentPackages = characterRepository.getAllPackages();

            int size = currentPackages.size();
            String[] names = new String[size];
            String[] authors = new String[size]; // автора пока нет, заполняем пустыми строками
            int[] versions = new int[size];
            String[] descriptions = new String[size];

            for (int i = 0; i < size; i++) {
                Package pkg = currentPackages.get(i);
                names[i] = pkg.getName();
                authors[i] = ""; // можно заменить на что-то осмысленное, если появится
                // Преобразуем строковую версию в int (если возможно)
                try {
                    versions[i] = Integer.parseInt(pkg.getVersion());
                } catch (NumberFormatException e) {
                    versions[i] = 0; // если не число, ставим 0
                }
                descriptions[i] = pkg.getDescription();
            }

            rulesPackageList = new RulesPackageList(names, authors, versions, descriptions);
            rulesPackageList.setName("rulesPackageList");
            // Подписываемся на изменения выбора
            rulesPackageList.addSelectionChangeListener(this::updateDeleteButtonState);

            packagesScrollPane.setViewportView(rulesPackageList);
            packagesScrollPane.revalidate();
            packagesScrollPane.repaint();

            updateDeleteButtonState();

        } catch (SQLException e) {
            logger.error("Failed to load installed packages", e);
            JOptionPane.showMessageDialog(this,
                    localeManager.getString("installedPackages.error.load") + "\n" + e.getMessage(),
                    localeManager.getString("common.error"),
                    JOptionPane.ERROR_MESSAGE);
            rulesPackageList = null;
            packagesScrollPane.setViewportView(new JPanel());
            deletePackagesButton.setEnabled(false);
        }
    }

    private void updateDeleteButtonState() {
        if (rulesPackageList == null) {
            deletePackagesButton.setEnabled(false);
            return;
        }
        deletePackagesButton.setEnabled(!rulesPackageList.getSelectedIndices().isEmpty());
    }

    private void onAddPackage(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(localeManager.getString("installedPackages.add.chooser.title"));
        chooser.setFileFilter(new FileNameExtensionFilter("VTT Package Files (*.vttp)", "vttp"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            logger.info("Selected package file: {}", file.getAbsolutePath());
            // TODO: реализовать установку пакета
            JOptionPane.showMessageDialog(this,
                    localeManager.getString("installedPackages.add.stub") + "\n" + file.getName(),
                    localeManager.getString("common.info"),
                    JOptionPane.INFORMATION_MESSAGE);
            // После установки обновляем список
            refreshPackagesList();
        }
    }

    private void onDeletePackages(ActionEvent e) {
        if (rulesPackageList == null || currentPackages == null) return;
        List<Integer> selected = rulesPackageList.getSelectedIndices();
        if (selected.isEmpty()) return;

        String msg = localeManager.getString("installedPackages.delete.confirm")
                .replace("{0}", String.valueOf(selected.size()));
        int confirm = JOptionPane.showConfirmDialog(this,
                msg,
                localeManager.getString("installedPackages.delete.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        List<String> failed = new ArrayList<>();
        for (int index : selected) {
            Package pkg = currentPackages.get(index);
            try {
                characterRepository.deletePackage(pkg);
            } catch (Exception ex) {
                logger.error("Failed to delete package {}", pkg.getName(), ex);
                failed.add(pkg.getName());
            }
        }

        if (failed.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    localeManager.getString("installedPackages.delete.success"),
                    localeManager.getString("common.info"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    localeManager.getString("installedPackages.delete.error") + "\n" + String.join("\n", failed),
                    localeManager.getString("common.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
        refreshPackagesList();
    }
}