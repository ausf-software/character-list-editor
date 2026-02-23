package character_list_editor.window;

import character_list_editor.Main;
import character_list_editor.component.CharacterSheetList;
import character_list_editor.database.Character;
import character_list_editor.utils.CharacterLoadException;
import character_list_editor.utils.CharacterManager;
import character_list_editor.utils.LocaleManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

public class StartupFrame extends JFrame {
    private CharacterSheetList characterList;
    private Character selectedCharacter;

    public StartupFrame() {
        setTitle(LocaleManager.inst().getString("startup.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(800, 600);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(5);

        // Левая панель с кнопками
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        JButton createBtn = new JButton(LocaleManager.inst().getString("create"));
        JButton importBtn = new JButton(LocaleManager.inst().getString("import"));
        JButton settingsBtn = new JButton(LocaleManager.inst().getString("settings"));

        buttonPanel.add(createBtn);
        buttonPanel.add(importBtn);
        buttonPanel.add(settingsBtn);
        leftPanel.add(buttonPanel, BorderLayout.NORTH);

        JLabel versionLabel = new JLabel(
                LocaleManager.inst().getString("startup.version") + " " + Main.VERSION);
        leftPanel.add(versionLabel, BorderLayout.SOUTH);

        // Правая панель со списком и поиском
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель поиска
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        JTextField searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterCharacters(searchField.getText());
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterCharacters(searchField.getText());
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterCharacters(searchField.getText());
            }
        });
        searchPanel.add(new JLabel(LocaleManager.inst().getString("startup.search")), BorderLayout.NORTH);
        searchPanel.add(searchField, BorderLayout.CENTER);
        rightPanel.add(searchPanel, BorderLayout.NORTH);

        // Список персонажей
        characterList = new CharacterSheetList();
        rightPanel.add(new JScrollPane(characterList), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Обработчики кнопок
        createBtn.addActionListener(this::createNewCharacterList);
        importBtn.addActionListener(e -> importCharacter());
        settingsBtn.addActionListener(e -> openSettings());

        // Двойной клик или Enter по элементу списка
        characterList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedCharacter();
                }
            }
        });
        characterList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openSelectedCharacter();
                }
            }
        });
    }

    /**
     * Фильтрует список персонажей по введённому тексту.
     */
    private void filterCharacters(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // Пустая строка – сбрасываем фильтр
            characterList.setFilter(null);
        } else {

        }
    }


    private void createNewCharacterList(ActionEvent e) {
        // TODO: создание нового персонажа
    }

    private void openSettings() {
        SettingsDialog settingsDialog = new SettingsDialog(this);
        settingsDialog.setVisible(true);
    }

    private void openSelectedCharacter() {
        Character selected = characterList.getSelectedCharacter();
        if (selected != null) {
            selectedCharacter = selected;
            openCharacter(selectedCharacter);
        }
    }

    private void importCharacter() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
            if (selectedPath == null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        LocaleManager.inst().getString("character.import.error"),
                        LocaleManager.inst().getString("error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openCharacter(Character character) {
        try {
            CharacterFrame characterFrame = new CharacterFrame(character);
            characterFrame.setVisible(true);
            CharacterManager.getInstance().openCharacter(character.getSheetPath(), characterFrame);
            dispose();
        } catch (CharacterLoadException ex) {
            JOptionPane.showMessageDialog(this,
                    LocaleManager.inst().getString("character.load.error") + "\n" + ex.getMessage(),
                    LocaleManager.inst().getString("error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}