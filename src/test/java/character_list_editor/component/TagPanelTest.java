package character_list_editor.component;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.Assert.*;


public class TagPanelTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;
    private JFrame frame;
    private TagPanel tagPanel;

    @Override
    protected void onSetUp() {
        // Создание TagPanel в потоке EDT
        tagPanel = GuiActionRunner.execute(() -> {
            TagPanel panel = new TagPanel(Color.RED, "Test");
            panel.setName("tagPanel");
            panel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            return panel;
        });

        // Создание и отображение фрейма
        frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("TagPanel Test");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.getContentPane().add(tagPanel);
            f.pack();
            return f;
        });

        window = new FrameFixture(robot(), frame);
        window.show();
    }

    @Override
    protected void onTearDown() {
        window.cleanUp();
    }

    // Вспомогательный метод для чтения приватного поля через рефлексию
    private <T> T getFieldValue(Object obj, String fieldName, Class<T> type) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(obj));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read field: " + fieldName, e);
        }
    }

    /**
     * Проверяет, что конструктор правильно устанавливает начальные значения:
     * текст, цвет фона и производный более тёмный цвет.
     */
    @Test
    public void testInitialProperties() {
        TagPanel panel = window.panel("tagPanel").targetCastedTo(TagPanel.class);

        assertEquals("Test", getFieldValue(panel, "text", String.class));
        assertEquals(Color.RED, getFieldValue(panel, "backgroundColor", Color.class));
        assertEquals(Color.RED.darker().darker(), getFieldValue(panel, "darkerColor", Color.class));
    }

    /**
     * Проверяет, что метод setText() обновляет внутреннее поле text,
     * а также пересчитывает предпочтительный размер (ширина должна увеличиться).
     */
    @Test
    public void testSetTextUpdatesFieldAndPreferredSize() {
        TagPanel panel = window.panel("tagPanel").targetCastedTo(TagPanel.class);
        Dimension oldSize = panel.getPreferredSize();

        GuiActionRunner.execute(() -> panel.setText("New longer text"));

        // Проверка обновления поля
        assertEquals("New longer text", getFieldValue(panel, "text", String.class));

        // Предпочтительный размер должен увеличиться (текст стал шире)
        Dimension newSize = panel.getPreferredSize();
        assertTrue(newSize.width > oldSize.width);
        assertEquals(newSize.height, oldSize.height); // высота для того же шрифта не меняется
    }

    /**
     * Проверяет, что метод setBackgroundColor() обновляет цвет фона и
     * производный более тёмный цвет, а компонент остаётся видимым.
     */
    @Test
    public void testSetBackgroundColorUpdatesColorsAndRepaints() {
        TagPanel panel = window.panel("tagPanel").targetCastedTo(TagPanel.class);

        GuiActionRunner.execute(() -> panel.setBackgroundColor(Color.BLUE));

        assertEquals(Color.BLUE, getFieldValue(panel, "backgroundColor", Color.class));
        assertEquals(Color.BLUE.darker().darker(), getFieldValue(panel, "darkerColor", Color.class));

        // Компонент должен остаться видимым (нет исключений)
        window.panel("tagPanel").requireVisible();
    }

    /**
     * Проверяет корректность расчёта предпочтительного размера:
     * ширина = ширина текста + 2 * горизонтальный отступ,
     * высота = высота текста + 2 * вертикальный отступ.
     */
    @Test
    public void testPreferredSizeCalculation() {
        TagPanel panel = window.panel("tagPanel").targetCastedTo(TagPanel.class);

        // Значения отступов из исходного кода TagPanel
        int hPadding = 10;   // horizontalPadding
        int vPadding = 5;    // verticalPadding

        FontMetrics fm = panel.getFontMetrics(panel.getFont());
        int textWidth = fm.stringWidth("Test");
        int textHeight = fm.getHeight();

        int expectedWidth = textWidth + 2 * hPadding;
        int expectedHeight = textHeight + 2 * vPadding;

        Dimension pref = panel.getPreferredSize();
        assertEquals(expectedWidth, pref.width);
        assertEquals(expectedHeight, pref.height);
    }
}
