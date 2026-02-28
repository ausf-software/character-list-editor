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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class StartupFrame extends JFrame {
    private CharacterSheetList characterList;
    private Character selectedCharacter;
    private JTextField searchField;
    private Timer searchTimer;

    public StartupFrame() {
        setTitle(LocaleManager.inst().getString("startup.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeUI();
        // После инициализации загружаем список персонажей
        characterList.refresh();
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

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        JButton createBtn = new JButton(LocaleManager.inst().getString("create"));
        JButton importBtn = new JButton(LocaleManager.inst().getString("import"));
        JButton settingsBtn = new JButton(LocaleManager.inst().getString("settings"));
        JButton packsBtn = new JButton(LocaleManager.inst().getString("installedPackages.title"));

        buttonPanel.add(createBtn);
        buttonPanel.add(importBtn);
        buttonPanel.add(packsBtn);
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
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleFilter();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleFilter();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleFilter();
            }
        });
        searchPanel.add(new JLabel(LocaleManager.inst().getString("startup.search")), BorderLayout.NORTH);
        searchPanel.add(searchField, BorderLayout.CENTER);
        rightPanel.add(searchPanel, BorderLayout.NORTH);

        // Список персонажей
        characterList = new CharacterSheetList();
        rightPanel.add(characterList, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Обработчики кнопок
        createBtn.addActionListener(this::createNewCharacterList);
        importBtn.addActionListener(e -> importCharacter());
        settingsBtn.addActionListener(e -> openSettings());
        packsBtn.addActionListener(e -> openPacksWindow());

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

        // Таймер для debounce поиска
        searchTimer = new Timer(300, e -> {
            // Выполняем фильтрацию с текущим текстом
            performFilter(searchField.getText());
        });
        searchTimer.setRepeats(false); // однократное срабатывание
    }

    private void openPacksWindow() {
        InstalledPackagesWindow packagesDialog = new InstalledPackagesWindow(this);
        packagesDialog.setVisible(true);
    }

    /**
     * Запланировать фильтрацию через 300 мс после последнего ввода.
     */
    private void scheduleFilter() {
        if (searchTimer.isRunning()) {
            searchTimer.restart();
        } else {
            searchTimer.start();
        }
    }

    /**
     * Фильтрует список персонажей по введённому тексту.
     */
    private void performFilter(String searchText) {
        try {
            if (searchText == null || searchText.trim().isEmpty()) {
                // Пустая строка – сбрасываем фильтр
                characterList.setFilter(null);
            } else {
                Predicate<Character> predicate = createPredicate(searchText);
                characterList.setFilter(predicate);
            }
        } catch (Exception ex) {
            // Логируем и показываем сообщение
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        LocaleManager.inst().getString("search.error") + "\n" + ex.getMessage(),
                        LocaleManager.inst().getString("error"),
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    /**
     * Создаёт предикат для фильтрации на основе строки запроса.
     * Поддерживает кавычки для фраз, регистронезависимость, поиск по подстроке.
     */
    private Predicate<Character> createPredicate(String query) {
        // Токенизация с учётом кавычек
        List<String> tokens = parseQuery(query.trim());
        if (tokens.isEmpty()) {
            return ch -> true; // все подходят, но в performFilter мы уже обработали пустой случай
        }

        return character -> {
            // Получаем поля персонажа в нижнем регистре для сравнения
            String name = character.getName() != null ? character.getName().toLowerCase() : "";
            String campaign = character.getCampaign() != null ? character.getCampaign().toLowerCase() : "";
            // Имена тегов
            List<String> tagNames = new ArrayList<>();
            if (character.getTags() != null) {
                for (var tag : character.getTags()) {
                    if (tag.getName() != null) {
                        tagNames.add(tag.getName().toLowerCase());
                    }
                }
            }

            // Для каждого токена проверяем вхождение хотя бы в одно поле
            for (String token : tokens) {
                boolean found = false;
                // Проверяем имя
                if (name.contains(token)) {
                    found = true;
                }
                // Проверяем кампанию
                if (!found && campaign.contains(token)) {
                    found = true;
                }
                // Проверяем теги
                if (!found) {
                    for (String tagName : tagNames) {
                        if (tagName.contains(token)) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    return false; // токен не найден
                }
            }
            return true;
        };
    }

    /**
     * Разбирает строку запроса на токены, учитывая кавычки.
     * Пример: "John Doe" campaign -> ["john doe", "campaign"]
     */
    private List<String> parseQuery(String query) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int len = query.length();
        while (i < len) {
            // Пропускаем пробелы
            if (java.lang.Character.isWhitespace(query.charAt(i))) {
                i++;
                continue;
            }

            if (query.charAt(i) == '"') {
                // Начало кавычки
                i++; // пропускаем открывающую кавычку
                int start = i;
                // Ищем закрывающую кавычку
                while (i < len && query.charAt(i) != '"') {
                    i++;
                }
                if (i < len && query.charAt(i) == '"') {
                    // Нашли закрывающую
                    String token = query.substring(start, i).toLowerCase();
                    tokens.add(token);
                    i++; // пропускаем закрывающую
                } else {
                    // Кавычка не закрыта, берем от start до конца
                    String token = query.substring(start).toLowerCase();
                    tokens.add(token);
                    break; // конец строки
                }
            } else {
                // Обычный токен до пробела
                int start = i;
                while (i < len && !java.lang.Character.isWhitespace(query.charAt(i))) {
                    i++;
                }
                String token = query.substring(start, i).toLowerCase();
                tokens.add(token);
            }
        }
        return tokens;
    }

    // Остальные методы без изменений...
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