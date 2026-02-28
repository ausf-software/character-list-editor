package character_list_editor.window;

import character_list_editor.utils.DiceType;
import character_list_editor.utils.LocaleManager;

import javax.swing.*;
import java.awt.*;

/**
 * Тестовый класс для визуального просмотра и проверки работы DiceRollPanel.
 * Демонстрирует различные варианты создания панели: с предустановленным типом куба,
 * со свободным выбором, с модификатором, с подсказкой.
 */
public class DiceRollPanelViewTest {

    public static void main(String[] args) {
        // Инициализация локализации (если нужно, можно принудительно установить локаль)
        // LocaleManager.inst().setLocale("ru");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Тест панели броска кубов");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Создаем панель с вкладками для разных конфигураций
            JTabbedPane tabbedPane = new JTabbedPane();

            // 1. Полная конфигурация: предустановленный куб d20, модификатор +2, подсказка
            DiceRollPanel fullPanel = new DiceRollPanel(DiceType.D20, 2, "Бросок атаки");
            tabbedPane.addTab("С预设 d20 +2", fullPanel);

            // 2. Свободный выбор куба, без модификатора, без подсказки
            DiceRollPanel freePanel = new DiceRollPanel(null, null, null);
            tabbedPane.addTab("Свободный выбор", freePanel);

            // 3. Предустановленный куб d6, без модификатора, с подсказкой
            DiceRollPanel hintOnlyPanel = new DiceRollPanel(DiceType.D6, null, "Урон огнём");
            tabbedPane.addTab("d6 с подсказкой", hintOnlyPanel);

            // 4. Предустановленный куб d100, с модификатором -5, без подсказки
            DiceRollPanel modifierOnlyPanel = new DiceRollPanel(DiceType.D100, -5, null);
            tabbedPane.addTab("d100 -5", modifierOnlyPanel);

            frame.add(tabbedPane);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null); // центр экрана
            frame.setVisible(true);
        });
    }
}