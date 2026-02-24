
package character_list_editor.component;

import character_list_editor.database.Character;
import character_list_editor.database.CharacterRepository;
import character_list_editor.database.Tag;
import character_list_editor.utils.LocaleManager;
import character_list_editor.window.TagEditWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CharacterSheetList extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(CharacterSheetList.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final LocaleManager localeManager;
    private final CharacterRepository repository;

    private JList<Character> characterList;
    private DefaultListModel<Character> listModel;
    private JScrollPane scrollPane;

    private List<Character> fullList = new ArrayList<>();
    private Predicate<Character> currentFilter;

    public CharacterSheetList() {
        super(new BorderLayout());
        this.localeManager = LocaleManager.inst();
        try {
            this.repository = CharacterRepository.getInstance();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize repository", e);
        }

        initComponents();
        refresh();
    }

    private void initComponents() {
        listModel = new DefaultListModel<>();
        characterList = new JList<>(listModel);
        characterList.setName("characterList");
        characterList.setCellRenderer(new CharacterCardRenderer());
        characterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Прототип для определения размера ячейки
        Character prototype = new Character();
        prototype.setName("Прототип с длинным именем для определения размера");
        prototype.setCampaign("Кампания");
        characterList.setPrototypeCellValue(prototype);

        scrollPane = new JScrollPane(characterList);
        scrollPane.setName("scrollPane");
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(scrollPane, BorderLayout.CENTER);
    }

    public void refresh() {
        try {
            fullList = repository.getAllCharacters();
            currentFilter = null;
            applyFilter();
            // Принудительное обновление отображения
            characterList.revalidate();
            characterList.repaint();
            scrollPane.revalidate();
            scrollPane.repaint();
        } catch (SQLException e) {
            logger.error("Failed to load characters", e);
            JOptionPane.showMessageDialog(this,
                    localeManager.getString("charactersheetlist.error.load") + "\n" + e.getMessage(),
                    localeManager.getString("common.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setFilter(Predicate<Character> filter) {
        this.currentFilter = filter;
        applyFilter();
        characterList.revalidate();
        characterList.repaint();
    }

    private void applyFilter() {
        List<Character> filtered = (currentFilter == null)
                ? fullList
                : fullList.stream().filter(currentFilter).collect(Collectors.toList());

        listModel.clear();
        filtered.forEach(listModel::addElement);
    }

    public Character getSelectedCharacter() {
        return characterList.getSelectedValue();
    }

    // ==================== Рендерер для карточек персонажей ====================

    private class CharacterCardRenderer implements ListCellRenderer<Character> {
        private final JPanel panel = new JPanel(new BorderLayout(5, 5));
        private final JPanel contentPanel = new JPanel(new GridBagLayout());
        private final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        private final JLabel characterNameLabel = new JLabel();
        private final JLabel campaignLabel = new JLabel();
        private final JLabel lastOpenedLabel = new JLabel();
        private final JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        private final JButton addTagButton = new JButton();
        private final JButton viewPackagesButton = new JButton();
        private final JButton exportButton = new JButton();
        private final JButton deleteButton = new JButton();

        public CharacterCardRenderer() {
            characterNameLabel.setName("characterNameLabel");
            campaignLabel.setName("campaignLabel");
            lastOpenedLabel.setName("lastOpenedLabel");
            tagsPanel.setName("tagsPanel");
            addTagButton.setName("addTagButton");
            viewPackagesButton.setName("viewPackagesButton");
            exportButton.setName("exportButton");
            deleteButton.setName("deleteButton");

            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    new EmptyBorder(5, 5, 5, 5)
            ));
            panel.setOpaque(true);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(characterNameLabel, BorderLayout.WEST);
            topPanel.add(campaignLabel, BorderLayout.EAST);
            topPanel.setOpaque(false);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(2, 2, 2, 2);
            contentPanel.add(topPanel, gbc);

            gbc.gridy = 1;
            contentPanel.add(lastOpenedLabel, gbc);

            gbc.gridy = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            contentPanel.add(tagsPanel, gbc);

            buttonPanel.add(addTagButton);
            buttonPanel.add(viewPackagesButton);
            buttonPanel.add(exportButton);
            buttonPanel.add(deleteButton);

            panel.add(contentPanel, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            updateButtonTexts();
        }

        private void updateButtonTexts() {
            addTagButton.setText(localeManager.getString("charactersheetlist.button.addtag"));
            viewPackagesButton.setText(localeManager.getString("charactersheetlist.button.packages"));
            exportButton.setText(localeManager.getString("charactersheetlist.button.export"));
            deleteButton.setText(localeManager.getString("charactersheetlist.button.delete"));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Character> list,
                                                      Character character,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            if (character == null) return panel;

            characterNameLabel.setText(character.getName());
            campaignLabel.setText(character.getCampaign());
            lastOpenedLabel.setText(character.getLastOpened() != null
                    ? character.getLastOpened().format(DATE_FORMATTER)
                    : "");

            tagsPanel.removeAll();
            if (character.getTags() != null) {
                for (Tag tag : character.getTags()) {
                    TagPanel tagPanel = new TagPanel(tag.getColor(), tag.getName());
                    tagPanel.setName("tag_" + tag.getId());
                    tagsPanel.add(tagPanel);
                }
            }
            tagsPanel.revalidate();
            tagsPanel.repaint();

            updateButtonListeners(list, index);

            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                contentPanel.setBackground(list.getSelectionBackground());
                buttonPanel.setBackground(list.getSelectionBackground());
                tagsPanel.setBackground(list.getSelectionBackground());
            } else {
                panel.setBackground(list.getBackground());
                contentPanel.setBackground(list.getBackground());
                buttonPanel.setBackground(list.getBackground());
                tagsPanel.setBackground(list.getBackground());
            }

            // Принудительный пересчёт размера для текущего содержимого
            panel.invalidate();
            panel.validate();

            return panel;
        }

        private void updateButtonListeners(JList<? extends Character> list, int index) {
            for (ActionListener al : addTagButton.getActionListeners()) {
                addTagButton.removeActionListener(al);
            }
            for (ActionListener al : viewPackagesButton.getActionListeners()) {
                viewPackagesButton.removeActionListener(al);
            }
            for (ActionListener al : exportButton.getActionListeners()) {
                exportButton.removeActionListener(al);
            }
            for (ActionListener al : deleteButton.getActionListeners()) {
                deleteButton.removeActionListener(al);
            }

            addTagButton.addActionListener(e -> handleAddTag(list, index));
            viewPackagesButton.addActionListener(e -> handleViewPackages(list, index));
            exportButton.addActionListener(e -> handleExport(list, index));
            deleteButton.addActionListener(e -> handleDelete(list, index));
        }

        private Character getCharacterFromList(JList<? extends Character> list, int index) {
            if (index >= 0 && index < list.getModel().getSize()) {
                return list.getModel().getElementAt(index);
            }
            return null;
        }

        private void handleAddTag(JList<? extends Character> list, int index) {
            Character character = getCharacterFromList(list, index);
            if (character == null) return;

            Window window = SwingUtilities.getWindowAncestor(CharacterSheetList.this);
            TagEditWindow dialog = new TagEditWindow(window instanceof Frame ? (Frame) window : null, character);
            dialog.setVisible(true);
            refresh();
        }

        private void handleViewPackages(JList<? extends Character> list, int index) {
            Character character = getCharacterFromList(list, index);
            if (character == null) return;

            JOptionPane.showMessageDialog(CharacterSheetList.this,
                    "Функция просмотра пакетов будет доступна в следующей версии.",
                    "Информация",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        private void handleExport(JList<? extends Character> list, int index) {
            Character character = getCharacterFromList(list, index);
            if (character == null) return;

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle(localeManager.getString("charactersheetlist.export.chooser.title"));

            int result = chooser.showSaveDialog(CharacterSheetList.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File targetDir = chooser.getSelectedFile();
                File sourceFile = new File(character.getSheetPath());
                if (!sourceFile.exists()) {
                    JOptionPane.showMessageDialog(CharacterSheetList.this,
                            localeManager.getString("charactersheetlist.export.error.notfound"),
                            localeManager.getString("common.error"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                File targetFile = new File(targetDir, sourceFile.getName());
                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(CharacterSheetList.this,
                            localeManager.getString("charactersheetlist.export.success") + "\n" + targetFile.getPath(),
                            localeManager.getString("common.info"),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    logger.error("Export failed", e);
                    JOptionPane.showMessageDialog(CharacterSheetList.this,
                            localeManager.getString("charactersheetlist.export.error.io") + "\n" + e.getMessage(),
                            localeManager.getString("common.error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void handleDelete(JList<? extends Character> list, int index) {
            Character character = getCharacterFromList(list, index);
            if (character == null) return;

            String message = localeManager.getString("charactersheetlist.delete.confirm")
                    .replace("{0}", character.getName());
            int confirm = JOptionPane.showConfirmDialog(CharacterSheetList.this,
                    message,
                    localeManager.getString("charactersheetlist.delete.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    repository.deleteCharacter(character);
                    refresh();
                } catch (SQLException e) {
                    logger.error("Delete failed", e);
                    JOptionPane.showMessageDialog(CharacterSheetList.this,
                            localeManager.getString("charactersheetlist.error.delete") + "\n" + e.getMessage(),
                            localeManager.getString("common.error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
