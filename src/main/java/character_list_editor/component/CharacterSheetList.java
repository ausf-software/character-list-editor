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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private static final int BUTTON_WIDTH = 70;  // фиксированная ширина кнопки
    private static final int BUTTON_HEIGHT = 25; // фиксированная высота кнопки

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
        setupMouseListener();
    }

    private void initComponents() {
        listModel = new DefaultListModel<>();
        characterList = new JList<>(listModel);
        characterList.setName("characterList");
        characterList.setCellRenderer(new CharacterCardRenderer());
        characterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Убираем прототип, чтобы ячейки занимали всю доступную ширину
        characterList.setPrototypeCellValue(null);
        // Включаем перенос строк в ячейках (не влияет, но оставим)
        characterList.setFixedCellHeight(-1);

        scrollPane = new JScrollPane(characterList);
        scrollPane.setName("scrollPane");
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Обрабатывает клики мыши для эмуляции нажатия кнопок в карточке.
     */
    private void setupMouseListener() {
        characterList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;

                int index = characterList.locationToIndex(e.getPoint());
                if (index < 0 || index >= listModel.size()) return;

                Rectangle cellBounds = characterList.getCellBounds(index, index);
                if (cellBounds == null || !cellBounds.contains(e.getPoint())) return;

                // Определяем область кнопок (нижняя часть ячейки)
                int yInCell = e.getPoint().y - cellBounds.y;
                int cellHeight = cellBounds.height;
                // Высота панели кнопок примерно равна BUTTON_HEIGHT + отступы (5+5)
                int buttonPanelHeight = BUTTON_HEIGHT + 10;
                if (yInCell <= cellHeight - buttonPanelHeight) return; // клик выше кнопок

                // Определяем горизонтальную область кнопок
                int xInCell = e.getPoint().x - cellBounds.x;
                // Кнопки выровнены вправо, их общая ширина: 4 * (BUTTON_WIDTH + промежуток 5) - промежуток после последней?
                // В FlowLayout.RIGHT они располагаются справа с промежутками.
                // Упростим: кнопки занимают область справа шириной 4 * BUTTON_WIDTH + 3 * 5 (промежутки между) + отступ слева 5?
                // Но для клика нам нужно знать границы каждой кнопки.
                // Будем считать, что кнопки начинаются с координаты buttonStartX и каждая имеет ширину BUTTON_WIDTH.
                // Отступ справа и слева от панели кнопок — по 5 пикселей.
                int totalButtonsWidth = 4 * BUTTON_WIDTH + 3 * 5; // 4 кнопки и 3 промежутка
                int buttonStartX = cellBounds.width - totalButtonsWidth - 5; // отступ справа 5

                if (xInCell < buttonStartX) return; // левее кнопок

                int buttonIndex = (xInCell - buttonStartX) / (BUTTON_WIDTH + 5); // ширина + промежуток
                if (buttonIndex < 0 || buttonIndex >= 4) return;

                Character character = listModel.getElementAt(index);
                switch (buttonIndex) {
                    case 0: handleAddTag(character); break;
                    case 1: handleViewPackages(character); break;
                    case 2: handleExport(character); break;
                    case 3: handleDelete(character); break;
                }
            }
        });
    }

    private void handleAddTag(Character character) {
        Window window = SwingUtilities.getWindowAncestor(this);
        TagEditWindow dialog = new TagEditWindow(window instanceof Frame ? (Frame) window : null, character);
        dialog.setVisible(true);
        refresh();
    }

    private void handleViewPackages(Character character) {
        // TODO: использовать RulesPackageList
        JOptionPane.showMessageDialog(this,
                "Функция просмотра пакетов будет доступна в следующей версии.",
                "Информация",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleExport(Character character) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(localeManager.getString("charactersheetlist.export.chooser.title"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File targetDir = chooser.getSelectedFile();
            File sourceFile = new File(character.getSheetPath());
            if (!sourceFile.exists()) {
                JOptionPane.showMessageDialog(this,
                        localeManager.getString("charactersheetlist.export.error.notfound"),
                        localeManager.getString("common.error"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            File targetFile = new File(targetDir, sourceFile.getName());
            try {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this,
                        localeManager.getString("charactersheetlist.export.success") + "\n" + targetFile.getPath(),
                        localeManager.getString("common.info"),
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.error("Export failed", e);
                JOptionPane.showMessageDialog(this,
                        localeManager.getString("charactersheetlist.export.error.io") + "\n" + e.getMessage(),
                        localeManager.getString("common.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDelete(Character character) {
        String message = localeManager.getString("charactersheetlist.delete.confirm")
                .replace("{0}", character.getName());
        int confirm = JOptionPane.showConfirmDialog(this,
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
                JOptionPane.showMessageDialog(this,
                        localeManager.getString("charactersheetlist.error.delete") + "\n" + e.getMessage(),
                        localeManager.getString("common.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refresh() {
        try {
            fullList = repository.getAllCharacters();
            currentFilter = null;
            applyFilter();
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

    @Override
    public void updateUI() {
        super.updateUI();
        if (characterList != null) {
            characterList.setCellRenderer(new CharacterCardRenderer());
        }
    }

    // ==================== Рендерер карточки персонажа ====================

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

            // Отключаем фокус на кнопках
            addTagButton.setFocusable(false);
            viewPackagesButton.setFocusable(false);
            exportButton.setFocusable(false);
            deleteButton.setFocusable(false);

            // Устанавливаем фиксированный размер кнопок для единообразия и точного определения кликов
            Dimension buttonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
            addTagButton.setPreferredSize(buttonSize);
            viewPackagesButton.setPreferredSize(buttonSize);
            exportButton.setPreferredSize(buttonSize);
            deleteButton.setPreferredSize(buttonSize);
            // Минимальный размер тоже фиксируем, чтобы избежать сжатия
            addTagButton.setMinimumSize(buttonSize);
            viewPackagesButton.setMinimumSize(buttonSize);
            exportButton.setMinimumSize(buttonSize);
            deleteButton.setMinimumSize(buttonSize);

            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    new EmptyBorder(5, 5, 5, 5)
            ));
            panel.setOpaque(true);

            // Верхняя панель с именем и кампанией
            JPanel topPanel = new JPanel(new GridBagLayout());
            GridBagConstraints topGbc = new GridBagConstraints();
            topGbc.insets = new Insets(0, 0, 0, 5);
            topGbc.gridx = 0;
            topGbc.gridy = 0;
            topGbc.anchor = GridBagConstraints.WEST;
            topGbc.fill = GridBagConstraints.HORIZONTAL;
            topGbc.weightx = 1.0;
            topPanel.add(characterNameLabel, topGbc);

            topGbc.gridx = 1;
            topGbc.weightx = 0.0;
            topGbc.fill = GridBagConstraints.NONE;
            topGbc.anchor = GridBagConstraints.EAST;
            topPanel.add(campaignLabel, topGbc);

            // Размещение в contentPanel
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

            // Кнопки
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
            characterNameLabel.setToolTipText(character.getName());

            campaignLabel.setText(character.getCampaign());
            campaignLabel.setToolTipText(character.getCampaign());

            lastOpenedLabel.setText(character.getLastOpened() != null
                    ? character.getLastOpened().format(DATE_FORMATTER)
                    : "");

            // Очистка и добавление тегов
            tagsPanel.removeAll();
            if (character.getTags() != null) {
                for (Tag tag : character.getTags()) {
                    TagPanel tagPanel = new TagPanel(tag.getColor(), tag.getName());
                    tagPanel.setName("tag_" + tag.getId());
                    tagPanel.setToolTipText(tag.getName());
                    tagsPanel.add(tagPanel);
                }
            }
            tagsPanel.revalidate();
            tagsPanel.repaint();

            // Цвета выделения
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

            panel.invalidate();
            panel.validate();
            return panel;
        }
    }
}