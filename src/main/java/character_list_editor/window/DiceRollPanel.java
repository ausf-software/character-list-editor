package character_list_editor.window;

import character_list_editor.utils.DiceType;
import character_list_editor.utils.LocaleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class DiceRollPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(DiceRollPanel.class);
    private static final int PREF_WIDTH = 300;
    private static final int PREF_HEIGHT = 200;
    private static final int ANIMATION_STEPS = 15;
    private static final int ANIMATION_DELAY = 100;

    private final LocaleManager localeManager = LocaleManager.inst();

    // Компоненты
    private JLabel hintLabel;
    private JPanel diceSelectionPanel;
    private JLabel diceTypeLabel;
    private JComboBox<DiceType> diceTypeComboBox;
    private JLabel fixedDiceTypeLabel;
    private JPanel modifierPanel;
    private JLabel modifierLabel;
    private JTextField modifierTextField;
    private JButton rollButton;
    private JPanel animationPanel;
    private JLabel animationLabel; // внутренний компонент для отображения чисел
    private JLabel resultLabel;
    private JLabel resultPrefixLabel;

    // Данные
    private final DiceType presetDiceType;
    private Integer currentModifier;
    private final String hint;
    private int lastRollResult; // результат последнего броска с модификатором
    private Timer animationTimer;
    private final Random random = new Random();

    public DiceRollPanel(DiceType presetDiceType, Integer modifier, String hint) {
        this.presetDiceType = presetDiceType;
        this.currentModifier = modifier;
        this.hint = hint;

        initComponents();
        setupLayout();
        updateVisibility();
        setPreferredSize(new Dimension(PREF_WIDTH, PREF_HEIGHT));
    }

    private void initComponents() {
        // Метка подсказки
        hintLabel = new JLabel();
        hintLabel.setName("hintLabel");
        if (hint != null) {
            hintLabel.setText(hint);
        }

        // Панель выбора куба
        diceSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        diceSelectionPanel.setName("diceSelectionPanel");

        diceTypeLabel = new JLabel(localeManager.getString("dice.type.label"));
        diceTypeLabel.setName("diceTypeLabel");

        diceTypeComboBox = new JComboBox<>(DiceType.values());
        diceTypeComboBox.setName("diceTypeComboBox");
        diceTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                if (value instanceof DiceType) {
                    value = ((DiceType) value).toString(); // отображаем как d4, d6...
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        diceSelectionPanel.add(diceTypeLabel);
        diceSelectionPanel.add(diceTypeComboBox);

        // Метка фиксированного типа
        fixedDiceTypeLabel = new JLabel();
        fixedDiceTypeLabel.setName("fixedDiceTypeLabel");
        if (presetDiceType != null) {
            fixedDiceTypeLabel.setText(presetDiceType.toString());
        }

        // Панель модификатора
        modifierPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        modifierPanel.setName("modifierPanel");

        modifierLabel = new JLabel(localeManager.getString("modifier.label"));
        modifierLabel.setName("modifierLabel");

        modifierTextField = new JTextField(5);
        modifierTextField.setName("modifierTextField");
        modifierTextField.setEditable(false);
        modifierTextField.setHorizontalAlignment(JTextField.RIGHT);

        modifierPanel.add(modifierLabel);
        modifierPanel.add(modifierTextField);

        // Кнопка броска
        rollButton = new JButton(localeManager.getString("roll.button"));
        rollButton.setName("rollButton");
        rollButton.addActionListener(e -> performRoll());

        // Панель анимации
        animationPanel = new JPanel(new BorderLayout());
        animationPanel.setName("animationPanel");
        animationPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        animationPanel.setPreferredSize(new Dimension(100, 50));

        animationLabel = new JLabel(" ", SwingConstants.CENTER);
        animationLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        animationPanel.add(animationLabel, BorderLayout.CENTER);

        // Метка результата
        resultPrefixLabel = new JLabel(localeManager.getString("result.label"));
        resultPrefixLabel.setName("resultPrefixLabel");

        resultLabel = new JLabel("--");
        resultLabel.setName("resultLabel");
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Устанавливаем значения модификатора, если задан
        if (currentModifier != null) {
            updateModifierField(currentModifier);
        }
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // Строка подсказки
        if (hint != null) {
            add(hintLabel, gbc);
            gbc.gridy++;
        }

        // Строка выбора куба или фиксированный тип
        if (presetDiceType == null) {
            add(diceSelectionPanel, gbc);
        } else {
            add(fixedDiceTypeLabel, gbc);
        }
        gbc.gridy++;

        // Строка модификатора
        if (currentModifier != null) {
            add(modifierPanel, gbc);
            gbc.gridy++;
        }

        // Кнопка броска
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        add(rollButton, gbc);

        // Пустая ячейка для отступа (можно использовать для результата)
        gbc.gridx = 1;
        add(new JLabel(), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        // Панель анимации
        add(animationPanel, gbc);
        gbc.gridy++;

        // Строка результата
        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        resultPanel.add(resultPrefixLabel);
        resultPanel.add(resultLabel);
        add(resultPanel, gbc);
    }

    private void updateVisibility() {
        // Подсказка уже добавлена/не добавлена при построении
        // Для простоты управляем видимостью через конструктор, но можно и здесь скрывать, если hint null
        if (hint == null) {
            hintLabel.setVisible(false);
        }

        // Выбор куба или фиксированный тип
        diceSelectionPanel.setVisible(presetDiceType == null);
        fixedDiceTypeLabel.setVisible(presetDiceType != null);

        // Модификатор
        modifierPanel.setVisible(currentModifier != null);
    }

    private void updateModifierField(int modifier) {
        String sign = modifier >= 0 ? "+" : "";
        modifierTextField.setText(sign + modifier);
    }

    private void performRoll() {
        // Останавливаем текущую анимацию, если идёт
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // Определяем текущий тип куба
        DiceType currentDiceType = getSelectedDiceType();
        if (currentDiceType == null) {
            // Не должно происходить, но на всякий случай
            logger.error("No dice type selected");
            JOptionPane.showMessageDialog(this,
                    localeManager.getString("error.no.dice.type"),
                    localeManager.getString("error.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int maxValue = currentDiceType.getMaxValue();
        if (maxValue <= 0) {
            logger.error("Invalid dice max value: {}", maxValue);
            JOptionPane.showMessageDialog(this,
                    localeManager.getString("error.invalid.dice"),
                    localeManager.getString("error.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Запускаем анимацию
        animationTimer = new Timer(ANIMATION_DELAY, null);
        final int[] step = {0};
        animationTimer.addActionListener(e -> {
            // Генерируем случайное значение на текущем шаге
            int randomValue = random.nextInt(maxValue) + 1;
            animationLabel.setText(String.valueOf(randomValue));

            step[0]++;
            if (step[0] >= ANIMATION_STEPS) {
                animationTimer.stop();
                // Финальный результат
                int diceRoll = random.nextInt(maxValue) + 1;
                int finalResult = diceRoll;
                if (currentModifier != null) {
                    finalResult += currentModifier;
                }
                lastRollResult = finalResult;
                resultLabel.setText(String.valueOf(finalResult));
                // Можно показать финальное значение в анимации
                animationLabel.setText(String.valueOf(diceRoll));
            }
        });

        animationTimer.start();
    }

    // Публичные методы

    /**
     * Возвращает последний результат броска (с модификатором).
     */
    public int getLastRollResult() {
        return lastRollResult;
    }

    /**
     * Возвращает текущий выбранный тип куба.
     */
    public DiceType getSelectedDiceType() {
        if (presetDiceType != null) {
            return presetDiceType;
        }
        return (DiceType) diceTypeComboBox.getSelectedItem();
    }

    /**
     * Устанавливает модификатор программно.
     * @param modifier новое значение модификатора (может быть null)
     */
    public void setModifier(Integer modifier) {
        this.currentModifier = modifier;
        if (modifier == null) {
            modifierPanel.setVisible(false);
        } else {
            modifierPanel.setVisible(true);
            updateModifierField(modifier);
        }
        revalidate();
        repaint();
    }

    // Для тестирования можно добавить геттеры компонентов
    public JLabel getHintLabel() { return hintLabel; }
    public JPanel getDiceSelectionPanel() { return diceSelectionPanel; }
    public JLabel getDiceTypeLabel() { return diceTypeLabel; }
    public JComboBox<DiceType> getDiceTypeComboBox() { return diceTypeComboBox; }
    public JLabel getFixedDiceTypeLabel() { return fixedDiceTypeLabel; }
    public JPanel getModifierPanel() { return modifierPanel; }
    public JLabel getModifierLabel() { return modifierLabel; }
    public JTextField getModifierTextField() { return modifierTextField; }
    public JButton getRollButton() { return rollButton; }
    public JPanel getAnimationPanel() { return animationPanel; }
    public JLabel getResultLabel() { return resultLabel; }
    public JLabel getResultPrefixLabel() { return resultPrefixLabel; }
}