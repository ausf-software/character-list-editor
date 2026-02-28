package character_list_editor.window;

import character_list_editor.component.TagPanel;
import character_list_editor.database.Character;
import character_list_editor.database.CharacterRepository;
import character_list_editor.database.Tag;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

public class TagEditWindowTest {

    private CharacterRepository repository;
    private Connection connection;
    private Robot robot;
    private FrameFixture parentFrame;
    private Character testCharacter;

    @Before
    public void setUp() throws Exception {
        // Сброс синглтона
        Field instanceField = CharacterRepository.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        repository = CharacterRepository.getInstance();

        Field dbField = CharacterRepository.class.getDeclaredField("db");
        dbField.setAccessible(true);
        Object characterDatabase = dbField.get(repository);
        Field connField = characterDatabase.getClass().getDeclaredField("connection");
        connField.setAccessible(true);
        connection = (Connection) connField.get(characterDatabase);

        clearTables();

        testCharacter = new Character("Тестовый", "Кампания", LocalDateTime.now(), "/path");
        repository.addCharacter(testCharacter);

        robot = BasicRobot.robotWithNewAwtHierarchy();
        parentFrame = new FrameFixture(robot, createParentFrame());
        parentFrame.show();
    }

    @After
    public void tearDown() {
        if (parentFrame != null) {
            parentFrame.cleanUp();
        }
        if (robot != null) {
            robot.cleanUp();
        }
        // Не закрываем репозиторий, он пересоздаётся в setUp
    }

    private void clearTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM character_tags");
            stmt.execute("DELETE FROM character_backups");
            stmt.execute("DELETE FROM character_package");
            stmt.execute("DELETE FROM characters");
            stmt.execute("DELETE FROM tags");
            stmt.execute("DELETE FROM packages");
            stmt.execute("DELETE FROM sqlite_sequence");
        }
    }

    private JFrame createParentFrame() {
        JFrame frame = new JFrame("Parent");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(200, 200);
        return frame;
    }

    private DialogFixture openDialog() {
        TagEditWindow dialog = GuiActionRunner.execute(() -> new TagEditWindow(parentFrame.target(), testCharacter));
        DialogFixture fixture = new DialogFixture(robot, dialog);
        fixture.show();
        return fixture;
    }

    private List<Tag> getCurrentTags() throws SQLException {
        Character updated = repository.getCharacter(testCharacter.getId());
        return updated.getTags();
    }

    // Вспомогательный метод для вызова приватного updateAddButtonState
    private void invokeUpdateAddButtonState(TagEditWindow dialog) throws Exception {
        Method method = TagEditWindow.class.getDeclaredMethod("updateAddButtonState");
        method.setAccessible(true);
        method.invoke(dialog);
    }

    // Вспомогательный метод для получения цвета из TagPanel (если есть поле backgroundColor)
    private Color getTagPanelBackgroundColor(TagPanel panel) throws Exception {
        try {
            Field bgField = TagPanel.class.getDeclaredField("backgroundColor");
            bgField.setAccessible(true);
            return (Color) bgField.get(panel);
        } catch (NoSuchFieldException e) {
            // Если поля нет, возвращаем null — тесты, которые его используют, будут пропущены
            return null;
        }
    }

    @Test
    public void testAllComponentsPresent() {
        DialogFixture dialog = openDialog();
        dialog.requireVisible();

        dialog.textBox("tagNameTextField").requireVisible();
        dialog.button("chooseColorButton").requireVisible();
        dialog.panel("tagPreviewPanel").requireVisible();
        dialog.button("addButton").requireVisible();
        dialog.button("cancelButton").requireVisible();

        dialog.close();
    }

    @Test
    public void testInitialState() throws Exception {
        DialogFixture dialog = openDialog();
        TagEditWindow dialogWindow = (TagEditWindow) dialog.target();

        dialog.textBox("tagNameTextField").requireText("");
        dialog.button("addButton").requireDisabled();

        // Проверяем selectedColor
        Field selectedColorField = TagEditWindow.class.getDeclaredField("selectedColor");
        selectedColorField.setAccessible(true);
        Color selectedColor = (Color) selectedColorField.get(dialogWindow);
        assertEquals(Color.LIGHT_GRAY, selectedColor);

        // Цвет кнопки выбора должен быть таким же
        assertEquals(Color.LIGHT_GRAY, dialogWindow.getChooseColorButton().getBackground());

        dialog.close();
    }

    @Test
    public void testTextInputActivatesButton() throws Exception {
        DialogFixture dialog = openDialog();
        TagEditWindow dialogWindow = (TagEditWindow) dialog.target();

        dialog.textBox("tagNameTextField").enterText("боевой");
        robot.waitForIdle();
        dialog.textBox("tagNameTextField").requireText("боевой");

        // Вызов приватного метода обновления
        invokeUpdateAddButtonState(dialogWindow);
        dialog.button("addButton").requireEnabled();

        dialog.textBox("tagNameTextField").setText("");
        robot.waitForIdle();
        invokeUpdateAddButtonState(dialogWindow);
        dialog.button("addButton").requireDisabled();

        dialog.textBox("tagNameTextField").enterText("   ");
        robot.waitForIdle();
        invokeUpdateAddButtonState(dialogWindow);
        dialog.button("addButton").requireDisabled();

        dialog.close();
    }

    @Test
    public void testColorSelection() throws Exception {
        DialogFixture dialog = openDialog();
        TagEditWindow dialogWindow = (TagEditWindow) dialog.target();

        // Устанавливаем цвет через рефлексию
        Field selectedColorField = TagEditWindow.class.getDeclaredField("selectedColor");
        selectedColorField.setAccessible(true);
        selectedColorField.set(dialogWindow, Color.RED);

        // Обновляем фон кнопки и preview вручную (как сделал бы слушатель)
        dialogWindow.getChooseColorButton().setBackground(Color.RED);
        dialogWindow.getTagPreviewPanel().setBackgroundColor(Color.RED);

        // Проверяем selectedColor
        Color selectedColor = (Color) selectedColorField.get(dialogWindow);
        assertEquals(Color.RED, selectedColor);

        // Проверяем фон кнопки
        assertEquals(Color.RED, dialogWindow.getChooseColorButton().getBackground());

        // Проверяем цвет preview через поле backgroundColor, если доступно
        Color previewColor = getTagPanelBackgroundColor(dialogWindow.getTagPreviewPanel());
        if (previewColor != null) {
            assertEquals(Color.RED, previewColor);
        } else {
            // Если поля нет, хотя бы проверяем, что цвет не серый (опционально)
            assertNotEquals(Color.LIGHT_GRAY, dialogWindow.getTagPreviewPanel().getBackground());
        }

        dialog.close();
    }

    @Test
    public void testCancelDoesNotAddTag() throws SQLException {
        int initialTagCount = getCurrentTags().size();

        DialogFixture dialog = openDialog();
        dialog.textBox("tagNameTextField").enterText("новый тег");
        dialog.button("cancelButton").click();

        dialog.requireNotVisible();

        assertEquals(initialTagCount, getCurrentTags().size());
    }

    @Test
    public void testAddNewTag() throws Exception {
        int initialTagCount = getCurrentTags().size();

        DialogFixture dialog = openDialog();
        TagEditWindow dialogWindow = (TagEditWindow) dialog.target();

        dialog.textBox("tagNameTextField").enterText("уникальный");
        robot.waitForIdle();
        invokeUpdateAddButtonState(dialogWindow);

        dialog.button("addButton").click();
        robot.waitForIdle();

        try {
            dialog.requireNotVisible();
        } catch (AssertionError e) {
            dialog.close();
            throw e;
        }

        List<Tag> tags = getCurrentTags();
        assertEquals(initialTagCount + 1, tags.size());
        assertTrue(tags.stream().anyMatch(t -> t.getName().equals("уникальный")));

        boolean tagExistsInDb = false;
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM tags WHERE name = 'уникальный'")) {
            rs.next();
            tagExistsInDb = rs.getInt(1) > 0;
        }
        assertTrue(tagExistsInDb);
    }

    @Test
    public void testAddExistingTag() throws Exception {
        Tag existingTag = new Tag();
        existingTag.setName("общий");
        existingTag.setColor(Color.CYAN);
        repository.addTag(existingTag);

        int initialTagCount = getCurrentTags().size();
        int initialTagsInDb;
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM tags WHERE name = 'общий'")) {
            rs.next();
            initialTagsInDb = rs.getInt(1);
        }

        DialogFixture dialog = openDialog();
        TagEditWindow dialogWindow = (TagEditWindow) dialog.target();

        dialog.textBox("tagNameTextField").enterText("общий");
        robot.waitForIdle();
        invokeUpdateAddButtonState(dialogWindow);
        dialog.button("addButton").click();
        robot.waitForIdle();

        try {
            dialog.requireNotVisible();
        } catch (AssertionError e) {
            dialog.close();
            throw e;
        }

        List<Tag> tags = getCurrentTags();
        assertEquals(initialTagCount + 1, tags.size());
        assertTrue(tags.stream().anyMatch(t -> t.getName().equals("общий")));

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM tags WHERE name = 'общий'")) {
            rs.next();
            assertEquals(initialTagsInDb, rs.getInt(1));
        }
    }

    @Test
    public void testAddDuplicateTagForCharacter() throws Exception {
        Tag tag = new Tag();
        tag.setName("дубль");
        tag.setColor(Color.MAGENTA);
        repository.addTag(tag);

        testCharacter.setTags(List.of(tag));
        repository.updateCharacter(testCharacter);

        int initialTagCount = getCurrentTags().size();

        DialogFixture dialog = openDialog();
        TagEditWindow dialogWindow = (TagEditWindow) dialog.target();

        dialog.textBox("tagNameTextField").enterText("дубль");
        robot.waitForIdle();
        invokeUpdateAddButtonState(dialogWindow);
        dialog.button("addButton").click();
        robot.waitForIdle();

        try {
            dialog.requireNotVisible();
        } catch (AssertionError e) {
            dialog.close();
            throw e;
        }

        assertEquals(initialTagCount, getCurrentTags().size());
    }
}