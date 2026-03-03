package character_list_editor.database;

import io.qameta.allure.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Набор автотестов для CharacterRepository согласно требованиям.
 * Использует реальную SQLite базу данных, но все таблицы очищаются перед каждым тестом.
 * Доступ к Connection осуществляется через рефлексию, так как CharacterDatabase не предоставляет публичного геттера.
 */
@Epic("База данных файлов")
@Feature("Управление файлами персонажей и пакетов правил\\контента")
@Owner("https://github.com/kalininasofa")
@Severity(SeverityLevel.CRITICAL)
@Link(name = "Тест-кейс", url = "https://github.com/ausf-software/character-list-editor/issues/32")
public class CharacterRepositoryTest {

    private CharacterRepository repository;
    private Connection connection;

    @Before
    @Step("Подготовка окружения: получение соединения и очистка таблиц")
    public void setUp() throws Exception {
        repository = CharacterRepository.getInstance();

        // Получаем доступ к соединению через рефлексию
        Field dbField = CharacterRepository.class.getDeclaredField("db");
        dbField.setAccessible(true);
        CharacterDatabase characterDatabase = (CharacterDatabase) dbField.get(repository);

        Field connField = CharacterDatabase.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connection = (Connection) connField.get(characterDatabase);

        clearTables();
    }

    @After
    @Step("Завершение теста (без закрытия соединения)")
    public void tearDown() {
        // Не закрываем репозиторий здесь, чтобы тесты могли работать дальше.
        // Для теста на закрытие соединения используется отдельный метод.
    }

    @Step("Очистка всех таблиц и сброс автоинкремента")
    private void clearTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM character_backups");
            stmt.execute("DELETE FROM character_tags");
            stmt.execute("DELETE FROM character_package");
            stmt.execute("DELETE FROM characters");
            stmt.execute("DELETE FROM tags");
            stmt.execute("DELETE FROM packages");
            stmt.execute("DELETE FROM sqlite_sequence"); // сброс автоинкремента
        }
    }

    // ==================== 2. Инициализация и состояние репозитория ====================

    @Test
    @Story("Инициализация")
    @Description("Проверка, что репозиторий реализует паттерн Singleton")
    public void testSingleton() throws SQLException {
        CharacterRepository instance2 = CharacterRepository.getInstance();
        assertSame(repository, instance2);
    }

    @Test
    @Story("Инициализация")
    @Description("Проверка, что все необходимые таблицы и колонки созданы при первом запуске")
    public void testTablesCreated() throws SQLException {
        String[] tables = {"characters", "packages", "character_package", "tags", "character_tags", "character_backups"};
        for (String table : tables) {
            try (var rs = connection.getMetaData().getTables(null, null, table, null)) {
                assertTrue("Таблица " + table + " не найдена", rs.next());
            }
        }

        // Проверка колонок таблицы characters
        String[] columns = {"id", "name", "campaign", "last_opened", "sheet_path"};
        for (String col : columns) {
            try (var rs = connection.getMetaData().getColumns(null, null, "characters", col)) {
                assertTrue("Колонка " + col + " не найдена", rs.next());
            }
        }
    }

    // ==================== 3. Тестирование операций с персонажами ====================

    @Test
    @Story("Управление персонажами")
    @Description("Добавление нового персонажа: проверка генерации ID и сохранения всех полей")
    @Severity(SeverityLevel.CRITICAL)
    public void testAddCharacter() throws SQLException {
        Character character = createCharacter("Aragorn", "Fellowship", LocalDateTime.now(), "/path/sheet.pdf");
        addCharacter(character);

        assertCharacterHasId(character);
        Character loaded = loadCharacter(character.getId());
        assertCharacterEquals(loaded, "Aragorn", "Fellowship", "/path/sheet.pdf");
        assertCollectionsAreEmpty(loaded);
    }

    @Step("Создание персонажа с именем {0}, кампанией {1}, датой {2}, путём {3}")
    private Character createCharacter(String name, String campaign, LocalDateTime lastOpened, String sheetPath) {
        return new Character(name, campaign, lastOpened, sheetPath);
    }

    @Step("Добавление персонажа в репозиторий")
    private void addCharacter(Character character) throws SQLException {
        repository.addCharacter(character);
    }

    @Step("Проверка, что персонажу присвоен положительный ID")
    private void assertCharacterHasId(Character character) {
        assertTrue(character.getId() > 0);
    }

    @Step("Загрузка персонажа из БД по ID = {0}")
    private Character loadCharacter(int id) throws SQLException {
        return repository.getCharacter(id);
    }

    @Step("Проверка полей персонажа: имя = {1}, кампания = {2}, путь = {3}")
    private void assertCharacterEquals(Character character, String expectedName, String expectedCampaign, String expectedSheetPath) {
        assertNotNull(character);
        assertEquals(expectedName, character.getName());
        assertEquals(expectedCampaign, character.getCampaign());
        assertEquals(expectedSheetPath, character.getSheetPath());
        assertNotNull(character.getLastOpened());
    }

    @Step("Проверка, что списки тегов, бэкапов и пакетов пусты")
    private void assertCollectionsAreEmpty(Character character) {
        assertNotNull(character.getTags());
        assertNotNull(character.getBackups());
        assertNotNull(character.getPackages());
        assertTrue(character.getTags().isEmpty());
        assertTrue(character.getBackups().isEmpty());
        assertTrue(character.getPackages().isEmpty());
    }

    @Test
    @Story("Управление персонажами")
    @Description("Получение персонажа по существующему ID")
    public void testGetCharacterById() throws SQLException {
        String insertSql = "INSERT INTO characters (name, campaign, last_opened, sheet_path) VALUES (?, ?, ?, ?)";
        try (var pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, "Legolas");
            pstmt.setString(2, "Fellowship");
            pstmt.setString(3, "2023-01-01 12:00:00");
            pstmt.setString(4, "/path/legolas.pdf");
            pstmt.executeUpdate();
        }

        int id;
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            id = rs.getInt(1);
        }

        Character loaded = repository.getCharacter(id);
        assertNotNull(loaded);
        assertEquals("Legolas", loaded.getName());
        assertEquals("Fellowship", loaded.getCampaign());
        assertEquals(LocalDateTime.of(2023, 1, 1, 12, 0), loaded.getLastOpened());
        assertEquals("/path/legolas.pdf", loaded.getSheetPath());
    }

    @Test
    @Story("Управление персонажами")
    @Description("Получение всех персонажей с сортировкой по last_opened (сначала новые)")
    public void testGetAllCharacters() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusDays(1);
        LocalDateTime latest = now.plusHours(1);

        Character c1 = new Character("Frodo", "Fellowship", earlier, "/p1");
        Character c2 = new Character("Sam", "Fellowship", now, "/p2");
        Character c3 = new Character("Gandalf", "Fellowship", latest, "/p3");

        repository.addCharacter(c1);
        repository.addCharacter(c2);
        repository.addCharacter(c3);

        List<Character> all = repository.getAllCharacters();
        assertEquals(3, all.size());

        assertEquals("Gandalf", all.get(0).getName());
        assertEquals("Sam", all.get(1).getName());
        assertEquals("Frodo", all.get(2).getName());

        for (Character c : all) {
            assertNotNull(c.getTags());
            assertNotNull(c.getBackups());
            assertNotNull(c.getPackages());
        }
    }

    @Test
    @Story("Управление персонажами")
    @Description("Обновление данных существующего персонажа")
    public void testUpdateCharacter() throws SQLException {
        Character character = new Character("Gimli", "Fellowship", LocalDateTime.now(), "/old");
        repository.addCharacter(character);

        character.setName("Gimli son of Gloin");
        character.setCampaign("Return of the King");
        character.setSheetPath("/new");

        repository.updateCharacter(character);

        Character updated = repository.getCharacter(character.getId());
        assertEquals("Gimli son of Gloin", updated.getName());
        assertEquals("Return of the King", updated.getCampaign());
        assertEquals("/new", updated.getSheetPath());
    }

    @Test
    @Story("Управление персонажами")
    @Description("Обновление времени последнего открытия персонажа")
    public void testUpdateLastOpened() throws SQLException {
        LocalDateTime oldDate = LocalDateTime.of(2020, 1, 1, 0, 0);
        Character character = new Character("Boromir", "Fellowship", oldDate, "/path");
        repository.addCharacter(character);

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        repository.updateLastOpened(character);

        LocalDateTime newDate = character.getLastOpened();
        assertTrue(newDate.isAfter(oldDate));

        Character fromDb = repository.getCharacter(character.getId());
        assertEquals(newDate.truncatedTo(ChronoUnit.SECONDS), fromDb.getLastOpened().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @Story("Управление персонажами")
    @Description("Удаление персонажа")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeleteCharacter() throws SQLException {
        Character character = new Character("Sauron", "Mordor", LocalDateTime.now(), "/evil");
        repository.addCharacter(character);
        int id = character.getId();

        repository.deleteCharacter(character);

        assertNull(repository.getCharacter(id));

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM characters WHERE id = " + id)) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    // ==================== 4. Тестирование работы с тегами ====================

    @Test
    @Story("Управление тегами")
    @Description("Добавление нового тега")
    public void testAddNewTag() throws SQLException {
        Tag tag = new Tag();
        tag.setName("important");
        tag.setColor(Color.RED);

        repository.addTag(tag);

        assertTrue(tag.getId() > 0);

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM tags WHERE name = 'important'")) {
            assertTrue(rs.next());
            assertEquals("important", rs.getString("name"));
            assertEquals(Color.RED.getRGB(), rs.getInt("color"));
        }
    }

    @Test
    @Story("Управление тегами")
    @Description("Попытка добавить тег с уже существующим именем — должен вернуть существующий ID, не меняя цвет")
    public void testAddExistingTag() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO tags (name, color) VALUES ('existing', " + Color.BLUE.getRGB() + ")");
        }

        Tag newTag = new Tag();
        newTag.setName("existing");
        newTag.setColor(Color.GREEN);

        repository.addTag(newTag); // метод изменяет переданный объект, устанавливая id
        int id = newTag.getId();

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT id, color FROM tags WHERE name = 'existing'")) {
            assertTrue(rs.next());
            assertEquals(id, rs.getInt("id"));
            assertEquals(Color.BLUE.getRGB(), rs.getInt("color"));
        }
    }

    @Test
    @Story("Управление тегами")
    @Description("Получение списка всех тегов (отсортированных по имени)")
    public void testGetAllTags() throws SQLException {
        Tag t1 = new Tag(); t1.setName("a"); t1.setColor(Color.RED);
        Tag t2 = new Tag(); t2.setName("b"); t2.setColor(Color.GREEN);
        repository.addTag(t1);
        repository.addTag(t2);

        List<Tag> all = repository.getAllTags();
        assertEquals(2, all.size());
        assertEquals("a", all.get(0).getName());
        assertEquals("b", all.get(1).getName());
    }

    // ==================== 5. Тестирование работы с пакетами ====================

    @Test
    @Story("Управление пакетами")
    @Description("Добавление нового пакета")
    public void testAddPackage() throws SQLException {
        Package pkg = new Package();
        pkg.setName("Core Rules");
        pkg.setDescription("Basic rules");
        pkg.setFilePath("/rules/core.pdf");
        pkg.setVersion("1.0");

        repository.addPackage(pkg);

        assertTrue(pkg.getId() > 0);

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM packages WHERE name = 'Core Rules'")) {
            assertTrue(rs.next());
            assertEquals("Core Rules", rs.getString("name"));
            assertEquals("Basic rules", rs.getString("description"));
            assertEquals("/rules/core.pdf", rs.getString("file_path"));
            assertEquals("1.0", rs.getString("version"));
        }
    }

    @Test
    @Story("Управление пакетами")
    @Description("Получение списка всех пакетов (отсортированных по имени)")
    public void testGetAllPackages() throws SQLException {
        Package p1 = new Package(); p1.setName("B"); p1.setDescription(""); p1.setFilePath(""); p1.setVersion("");
        Package p2 = new Package(); p2.setName("A"); p2.setDescription(""); p2.setFilePath(""); p2.setVersion("");
        repository.addPackage(p1);
        repository.addPackage(p2);

        List<Package> all = repository.getAllPackages();
        assertEquals(2, all.size());
        assertEquals("A", all.get(0).getName());
        assertEquals("B", all.get(1).getName());
    }

    @Test
    @Story("Управление пакетами")
    @Description("Удаление пакета, который не используется ни одним персонажем")
    public void testDeleteUnusedPackage() throws SQLException, PackageInUseException {
        Package pkg = new Package(); pkg.setName("Unused"); pkg.setDescription(""); pkg.setFilePath(""); pkg.setVersion("");
        repository.addPackage(pkg);

        repository.deletePackage(pkg);

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM packages WHERE id = " + pkg.getId())) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test(expected = PackageInUseException.class)
    @Story("Управление пакетами")
    @Description("Попытка удалить пакет, который используется персонажем, должна вызывать исключение")
    public void testDeleteUsedPackage_throwsException() throws SQLException, PackageInUseException {
        Package pkg = new Package(); pkg.setName("Used"); pkg.setDescription(""); pkg.setFilePath(""); pkg.setVersion("");
        repository.addPackage(pkg);

        Character character = new Character("Test", "TestCamp", LocalDateTime.now(), "/path");
        repository.addCharacter(character);
        character.setPackages(Collections.singletonList(pkg));
        repository.updateCharacter(character); // синхронизация добавит связь

        repository.deletePackage(pkg);
    }

    // ==================== 6. Тестирование работы с бэкапами ====================

    @Test
    @Story("Управление бэкапами")
    @Description("Добавление нового бэкапа для существующего персонажа")
    public void testAddBackup() throws SQLException {
        Character character = new Character("Test", "Test", LocalDateTime.now(), "/path");
        repository.addCharacter(character);

        Backup backup = new Backup();
        backup.setCharacterId(character.getId());
        backup.setBackupPath("/backup/1.zip");
        backup.setBackupDate(LocalDateTime.now());

        repository.addBackup(backup);

        assertTrue(backup.getId() > 0);

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM character_backups WHERE id = " + backup.getId())) {
            assertTrue(rs.next());
            assertEquals(character.getId(), rs.getInt("character_id"));
            assertEquals("/backup/1.zip", rs.getString("backup_path"));
        }
    }

    @Test
    @Story("Управление бэкапами")
    @Description("Получение списка бэкапов для персонажа (отсортированных по дате: сначала новые)")
    public void testGetBackupsForCharacter() throws SQLException {
        Character character = new Character("Test", "Test", LocalDateTime.now(), "/path");
        repository.addCharacter(character);

        LocalDateTime d1 = LocalDateTime.of(2023, 1, 1, 12, 0);
        LocalDateTime d2 = LocalDateTime.of(2023, 2, 1, 12, 0);

        Backup b1 = new Backup(); b1.setCharacterId(character.getId()); b1.setBackupPath("/b1"); b1.setBackupDate(d1);
        Backup b2 = new Backup(); b2.setCharacterId(character.getId()); b2.setBackupPath("/b2"); b2.setBackupDate(d2);
        repository.addBackup(b1);
        repository.addBackup(b2);

        List<Backup> backups = repository.getBackupsForCharacter(character.getId());
        assertEquals(2, backups.size());
        assertEquals(d2, backups.get(0).getBackupDate());
        assertEquals(d1, backups.get(1).getBackupDate());
    }

    @Test
    @Story("Управление бэкапами")
    @Description("Удаление существующего бэкапа")
    public void testDeleteBackup() throws SQLException {
        Character character = new Character("Test", "Test", LocalDateTime.now(), "/path");
        repository.addCharacter(character);

        Backup backup = new Backup();
        backup.setCharacterId(character.getId());
        backup.setBackupPath("/to_delete");
        backup.setBackupDate(LocalDateTime.now());
        repository.addBackup(backup);

        repository.deleteBackup(backup);

        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM character_backups WHERE id = " + backup.getId())) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    // ==================== 7. Тестирование синхронизации связей ====================

    @Test
    @Story("Синхронизация связей")
    @Description("Синхронизация тегов при обновлении персонажа: добавление новых, удаление отсутствующих")
    public void testSynchronizeTags() throws SQLException {
        Tag tagA = new Tag(); tagA.setName("А"); tagA.setColor(Color.RED);
        Tag tagB = new Tag(); tagB.setName("Б"); tagB.setColor(Color.GREEN);
        repository.addTag(tagA);
        repository.addTag(tagB);

        Character character = new Character("Test", "Test", LocalDateTime.now(), "/path");
        character.setTags(Arrays.asList(tagA, tagB));
        repository.addCharacter(character);

        character = repository.getCharacter(character.getId());

        Tag tagC = new Tag(); tagC.setName("В"); tagC.setColor(Color.BLUE);
        repository.addTag(tagC);

        character.setTags(Arrays.asList(tagB, tagC));
        repository.updateCharacter(character);

        List<Tag> tagsNow = repository.getCharacter(character.getId()).getTags();
        assertEquals(2, tagsNow.size());
        assertTrue(tagsNow.stream().anyMatch(t -> t.getName().equals("Б")));
        assertTrue(tagsNow.stream().anyMatch(t -> t.getName().equals("В")));
        assertFalse(tagsNow.stream().anyMatch(t -> t.getName().equals("А")));

        List<Tag> allTags = repository.getAllTags();
        assertEquals(3, allTags.size());
    }

    @Test
    @Story("Синхронизация связей")
    @Description("Синхронизация пакетов при обновлении персонажа")
    public void testSynchronizePackages() throws SQLException {
        Package p1 = new Package(); p1.setName("P1"); p1.setDescription(""); p1.setFilePath(""); p1.setVersion("");
        Package p2 = new Package(); p2.setName("P2"); p2.setDescription(""); p2.setFilePath(""); p2.setVersion("");
        Package p3 = new Package(); p3.setName("P3"); p3.setDescription(""); p3.setFilePath(""); p3.setVersion("");
        repository.addPackage(p1);
        repository.addPackage(p2);
        repository.addPackage(p3);

        Character character = new Character("Test", "Test", LocalDateTime.now(), "/path");
        character.setPackages(Arrays.asList(p1, p2));
        repository.addCharacter(character);

        character = repository.getCharacter(character.getId());
        character.setPackages(Arrays.asList(p1, p3));
        repository.updateCharacter(character);

        List<Package> packagesNow = repository.getCharacter(character.getId()).getPackages();
        assertEquals(2, packagesNow.size());
        assertTrue(packagesNow.stream().anyMatch(p -> p.getName().equals("P1")));
        assertTrue(packagesNow.stream().anyMatch(p -> p.getName().equals("P3")));
        assertFalse(packagesNow.stream().anyMatch(p -> p.getName().equals("P2")));
    }

    @Test
    @Story("Синхронизация связей")
    @Description("Синхронизация бэкапов при обновлении персонажа")
    public void testSynchronizeBackups() throws SQLException {
        Character character = new Character("Test", "Test", LocalDateTime.now(), "/path");
        repository.addCharacter(character);

        Backup b1 = new Backup(); b1.setCharacterId(character.getId()); b1.setBackupPath("/old1"); b1.setBackupDate(LocalDateTime.now().minusDays(1));
        Backup b2 = new Backup(); b2.setCharacterId(character.getId()); b2.setBackupPath("/old2"); b2.setBackupDate(LocalDateTime.now());
        repository.addBackup(b1);
        repository.addBackup(b2);

        character = repository.getCharacter(character.getId());

        b1.setBackupPath("/new1");
        Backup b3 = new Backup(); b3.setCharacterId(character.getId()); b3.setBackupPath("/new3"); b3.setBackupDate(LocalDateTime.now());
        character.setBackups(Arrays.asList(b1, b3));

        repository.updateCharacter(character);

        List<Backup> finalBackups = repository.getBackupsForCharacter(character.getId());
        assertEquals(2, finalBackups.size());

        Backup finalB1 = finalBackups.stream().filter(b -> b.getId() == b1.getId()).findFirst().orElse(null);
        assertNotNull(finalB1);
        assertEquals("/new1", finalB1.getBackupPath());

        assertTrue(finalBackups.stream().noneMatch(b -> b.getId() == b2.getId()));

        Backup finalB3 = finalBackups.stream().filter(b -> b.getId() != b1.getId()).findFirst().orElse(null);
        assertNotNull(finalB3);
        assertTrue(finalB3.getId() > 0);
        assertEquals("/new3", finalB3.getBackupPath());
    }

    // ==================== 8. Каскадное удаление ====================

    @Test
    @Story("Каскадное удаление")
    @Description("При удалении персонажа должны удаляться все связанные записи (теги, пакеты, бэкапы), но сами теги и пакеты остаются")
    public void testCascadeDeleteCharacter() throws SQLException {
        Tag tag = new Tag(); tag.setName("tag"); tag.setColor(Color.RED);
        repository.addTag(tag);
        Package pkg = new Package(); pkg.setName("pkg"); pkg.setDescription(""); pkg.setFilePath(""); pkg.setVersion("");
        repository.addPackage(pkg);

        Character character = new Character("Test", "Test", LocalDateTime.now(), "/path");
        character.setTags(Collections.singletonList(tag));
        character.setPackages(Collections.singletonList(pkg));
        repository.addCharacter(character);

        Backup backup = new Backup(); backup.setCharacterId(character.getId()); backup.setBackupPath("/back"); backup.setBackupDate(LocalDateTime.now());
        repository.addBackup(backup);

        repository.deleteCharacter(character);

        try (var stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM character_tags WHERE character_id = " + character.getId());
            rs.next();
            assertEquals(0, rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM character_package WHERE character_id = " + character.getId());
            rs.next();
            assertEquals(0, rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM character_backups WHERE character_id = " + character.getId());
            rs.next();
            assertEquals(0, rs.getInt(1));
        }

        assertFalse(repository.getAllTags().isEmpty());
        assertFalse(repository.getAllPackages().isEmpty());
    }

    // ==================== 9. Граничные случаи ====================

    @Test(expected = SQLException.class)
    @Story("Граничные случаи")
    @Description("Попытка добавить персонажа с именем null должна приводить к SQLException")
    @Severity(SeverityLevel.NORMAL)
    public void testAddCharacterWithNullName() throws SQLException {
        Character character = new Character(null, "Camp", LocalDateTime.now(), "/path");
        repository.addCharacter(character);
    }

    @Test
    @Story("Граничные случаи")
    @Description("Обновление несуществующего персонажа не должно вызывать исключений и не должно создавать запись")
    @Severity(SeverityLevel.NORMAL)
    public void testUpdateCharacterWithNonExistingId() throws SQLException {
        Character character = new Character("Ghost", "Nowhere", LocalDateTime.now(), "/void");
        character.setId(9999);
        repository.updateCharacter(character); // не должно быть исключения

        assertNull(repository.getCharacter(9999));
    }

    @Test(expected = SQLException.class)
    @Story("Граничные случаи")
    @Description("Добавление бэкапа для несуществующего персонажа должно вызывать SQLException")
    @Severity(SeverityLevel.NORMAL)
    public void testAddBackupForNonExistingCharacter() throws SQLException {
        Backup backup = new Backup();
        backup.setCharacterId(9999);
        backup.setBackupPath("/fake");
        backup.setBackupDate(LocalDateTime.now());
        repository.addBackup(backup);
    }
}