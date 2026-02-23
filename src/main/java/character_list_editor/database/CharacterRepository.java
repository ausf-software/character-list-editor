package character_list_editor.database;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Репозиторий для работы с персонажами, пакетами и тегами.
 * Реализован как синглтон. Предоставляет высокоуровневые методы,
 * автоматически загружающие связанные сущности (теги, бэкапы, пакеты)
 * и синхронизирующие связи при обновлении.
 */
public class CharacterRepository {
    private static CharacterRepository instance;
    private final CharacterDatabase db;

    private CharacterRepository() throws SQLException {
        this.db = new CharacterDatabase();
        this.db.connect();
        this.db.createTables();
    }

    public static synchronized CharacterRepository getInstance() throws SQLException {
        if (instance == null) {
            instance = new CharacterRepository();
        }
        return instance;
    }

    /**
     * Закрывает соединение с базой данных.
     * Следует вызвать при завершении работы приложения.
     */
    public void close() {
        db.disconnect();
    }

    // ==================== Персонажи ====================

    /**
     * Возвращает список всех персонажей, отсортированный по дате последнего открытия
     * (от самых свежих к самым старым). Каждый персонаж содержит полные списки
     * тегов, бэкапов и пакетов.
     */
    public List<Character> getAllCharacters() throws SQLException {
        List<Character> characters = db.getAllCharacters();
        for (Character c : characters) {
            loadCharacterDetails(c);
        }
        return characters;
    }

    /**
     * Возвращает персонажа по его ID со всеми связанными данными.
     */
    public Character getCharacter(int id) throws SQLException {
        Character character = db.getCharacter(id).orElse(null);
        if (character != null) {
            loadCharacterDetails(character);
        }
        return character;
    }

    /**
     * Обновляет дату последнего открытия персонажа на текущую и сохраняет изменения.
     * @param character объект персонажа (должен содержать корректный ID)
     * @return тот же объект с обновлённой датой
     */
    public Character updateLastOpened(Character character) throws SQLException {
        character.setLastOpened(LocalDateTime.now());
        db.updateCharacter(character);
        return character;
    }

    /**
     * Полностью обновляет данные персонажа, включая списки тегов, пакетов и бэкапов.
     * Синхронизирует связи с тегами и пакетами, а также синхронизирует список бэкапов
     * (добавляет новые, обновляет существующие, удаляет лишние).
     * @param character объект персонажа с актуальными списками (ID должен быть задан)
     * @return тот же объект (списки не изменяются, но у новых бэкапов будут проставлены ID)
     */
    public Character updateCharacter(Character character) throws SQLException {
        // 1. Обновляем основные поля
        db.updateCharacter(character);

        // 2. Синхронизируем теги
        synchronizeTags(character);

        // 3. Синхронизируем пакеты
        synchronizePackages(character);

        // 4. Синхронизируем бэкапы
        synchronizeBackups(character);

        // Перезагружаем полные данные, чтобы получить актуальные списки (включая ID новых бэкапов)
        loadCharacterDetails(character);
        return character;
    }

    /**
     * Добавляет нового персонажа в базу. Устанавливает сгенерированный ID в переданный объект.
     * После добавления также сохраняются все теги и пакеты, указанные в объекте.
     * @param character объект нового персонажа (ID игнорируется)
     * @return тот же объект с заполненным ID
     */
    public Character addCharacter(Character character) throws SQLException {
        // Сохраняем основную запись
        int id = db.addCharacter(character);
        character.setId(id);

        // Добавляем теги и пакеты
        if (character.getTags() != null) {
            for (Tag tag : character.getTags()) {
                db.addTagToCharacter(id, tag.getName(), tag.getColor());
            }
        }
        if (character.getPackages() != null) {
            for (Package pkg : character.getPackages()) {
                db.addPackageToCharacter(id, pkg.getId());
            }
        }

        loadCharacterDetails(character);
        return character;
    }

    /**
     * Удаляет персонажа вместе со всеми связями (теги, пакеты, бэкапы).
     * Сами теги и пакеты не удаляются.
     * @param character объект персонажа (достаточно наличия ID)
     */
    public void deleteCharacter(Character character) throws SQLException {
        db.deleteCharacter(character.getId());
    }

    // ==================== Теги ====================

    /**
     * Возвращает список всех тегов.
     */
    public List<Tag> getAllTags() throws SQLException {
        return db.getAllTags();
    }

    /**
     * Добавляет новый тег. Если тег с таким именем уже существует,
     * возвращает существующий (ID будет установлен в переданный объект).
     * @param tag объект тега (имя должно быть задано, ID игнорируется)
     * @return тот же объект с установленным ID
     */
    public Tag addTag(Tag tag) throws SQLException {
        int id = db.addTag(tag); // addTag возвращает ID существующего или нового
        tag.setId(id);
        return tag;
    }

    // ==================== Пакеты ====================

    /**
     * Возвращает список всех пакетов.
     */
    public List<Package> getAllPackages() throws SQLException {
        return db.getAllPackages();
    }

    /**
     * Добавляет новый пакет.
     * @param pkg объект пакета (все поля, кроме ID, должны быть заполнены)
     * @return тот же объект с установленным ID
     */
    public Package addPackage(Package pkg) throws SQLException {
        int id = db.addPackage(pkg);
        pkg.setId(id);
        return pkg;
    }

    /**
     * Удаляет пакет. Если пакет используется хотя бы одним персонажем,
     * выбрасывает исключение PackageInUseException.
     * @param pkg объект пакета (должен содержать ID)
     * @throws PackageInUseException если пакет привязан к персонажам
     */
    public void deletePackage(Package pkg) throws SQLException, PackageInUseException {
        List<Character> characters = db.getCharactersForPackage(pkg.getId());
        if (!characters.isEmpty()) {
            throw new PackageInUseException("Пакет \"" + pkg.getName() + "\" используется персонажами и не может быть удалён.");
        }
        db.deletePackage(pkg.getId());
    }

    // ==================== Бэкапы персонажей ====================

    /**
     * Возвращает список бэкапов для указанного персонажа.
     */
    public List<Backup> getBackupsForCharacter(int characterId) throws SQLException {
        return db.getBackupsForCharacter(characterId);
    }

    /**
     * Добавляет новый бэкап для персонажа.
     * @param backup объект бэкапа (characterId должен быть установлен, id игнорируется)
     * @return тот же объект с установленным id
     */
    public Backup addBackup(Backup backup) throws SQLException {
        int id = db.addBackup(backup);
        backup.setId(id);
        return backup;
    }

    /**
     * Удаляет бэкап.
     */
    public void deleteBackup(Backup backup) throws SQLException {
        db.deleteBackup(backup.getId());
    }

    /**
     * Удаляет бэкап по его идентификатору.
     */
    public void deleteBackup(int backupId) throws SQLException {
        db.deleteBackup(backupId);
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Загружает в объект персонажа связанные теги, бэкапы и пакеты.
     */
    private void loadCharacterDetails(Character character) throws SQLException {
        int id = character.getId();
        character.setTags(db.getTagObjectsForCharacter(id));
        character.setBackups(db.getBackupsForCharacter(id));
        character.setPackages(db.getPackagesForCharacter(id));
    }

    /**
     * Синхронизирует теги персонажа с базой данных.
     * Добавляет связи для тегов, которые есть в объекте, но отсутствуют в БД,
     * и удаляет связи для тегов, которых нет в объекте, но они есть в БД.
     */
    private void synchronizeTags(Character character) throws SQLException {
        int charId = character.getId();
        List<Tag> currentTagNames = db.getTagObjectsForCharacter(charId);
        List<Tag> newTagNames = character.getTags();

        // Удаляем лишние
        for (Tag tag : currentTagNames) {
            if (!newTagNames.contains(tag)) {
                db.removeTagFromCharacter(charId, tag.getName());
            }
        }
        // Добавляем новые
        for (Tag tag : newTagNames) {
            if (!currentTagNames.contains(tag)) {
                db.addTagToCharacter(charId, tag.getName(), tag.getColor());
            }
        }
    }

    /**
     * Синхронизирует бэкапы персонажа с базой данных.
     * Для каждого бэкапа из списка character.getBackups():
     * - если id == 0 -> создаётся новый (INSERT)
     * - если id > 0 -> обновляются поля backupPath и backupDate (UPDATE)
     * Бэкапы, присутствующие в БД, но отсутствующие в списке, удаляются.
     */
    private void synchronizeBackups(Character character) throws SQLException {
        int charId = character.getId();
        List<Backup> currentBackups = db.getBackupsForCharacter(charId);
        List<Backup> newBackups = character.getBackups() == null ? new ArrayList<>() : character.getBackups();

        // Составляем множества id для быстрого поиска
        Set<Integer> currentIds = currentBackups.stream().map(Backup::getId).collect(Collectors.toSet());
        Set<Integer> newIds = newBackups.stream().map(Backup::getId).filter(id -> id != 0).collect(Collectors.toSet());

        // 1. Удаляем те, что есть в БД, но отсутствуют в новом списке
        for (Integer id : currentIds) {
            if (!newIds.contains(id)) {
                db.deleteBackup(id);
            }
        }

        // 2. Обрабатываем каждый бэкап из нового списка
        for (Backup backup : newBackups) {
            backup.setCharacterId(charId); // гарантируем правильный characterId
            if (backup.getId() == 0) {
                // Новый бэкап
                int newId = db.addBackup(backup);
                backup.setId(newId);
            } else {
                // Существующий бэкап – обновляем
                db.updateBackup(backup);
            }
        }
    }

    /**
     * Синхронизирует пакеты персонажа с базой данных.
     * Добавляет связи для пакетов, которые есть в объекте, но отсутствуют в БД,
     * и удаляет связи для пакетов, которых нет в объекте, но они есть в БД.
     */
    private void synchronizePackages(Character character) throws SQLException {
        int charId = character.getId();
        List<Package> currentPackages = db.getPackagesForCharacter(charId);
        Set<Integer> currentIds = currentPackages.stream().map(Package::getId).collect(Collectors.toSet());
        List<Package> newPackages = character.getPackages() == null ?
                new ArrayList<>() :
                character.getPackages();
        Set<Integer> newIds = newPackages.stream().map(Package::getId).collect(Collectors.toSet());

        // Удаляем лишние
        for (Integer id : currentIds) {
            if (!newIds.contains(id)) {
                db.removePackageFromCharacter(charId, id);
            }
        }
        // Добавляем новые
        for (Integer id : newIds) {
            if (!currentIds.contains(id)) {
                db.addPackageToCharacter(charId, id);
            }
        }
    }
}