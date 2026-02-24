package character_list_editor.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final Logger logger = LoggerFactory.getLogger(CharacterRepository.class);
    private static CharacterRepository instance;
    private final CharacterDatabase db;

    private CharacterRepository() throws SQLException {
        logger.info("Initializing CharacterRepository instance");
        this.db = new CharacterDatabase();
        logger.debug("Connecting to database...");
        this.db.connect();
        logger.debug("Creating tables if not exist...");
        this.db.createTables();
        logger.info("CharacterRepository initialized successfully");
    }

    public static synchronized CharacterRepository getInstance() throws SQLException {
        if (instance == null) {
            logger.info("Creating new CharacterRepository instance");
            instance = new CharacterRepository();
        } else {
            logger.debug("Returning existing CharacterRepository instance");
        }
        return instance;
    }

    /**
     * Закрывает соединение с базой данных.
     * Следует вызвать при завершении работы приложения.
     */
    public void close() {
        logger.info("Closing database connection");
        db.disconnect();
        logger.debug("Database connection closed");
    }

    // ==================== Персонажи ====================

    /**
     * Возвращает список всех персонажей, отсортированный по дате последнего открытия
     * (от самых свежих к самым старым). Каждый персонаж содержит полные списки
     * тегов, бэкапов и пакетов.
     */
    public List<Character> getAllCharacters() throws SQLException {
        logger.debug("getAllCharacters() called");
        List<Character> characters = db.getAllCharacters();
        logger.debug("Loaded {} characters from database", characters.size());
        for (Character c : characters) {
            loadCharacterDetails(c);
        }
        logger.debug("Returning {} characters with full details", characters.size());
        return characters;
    }

    /**
     * Возвращает персонажа по его ID со всеми связанными данными.
     */
    public Character getCharacter(int id) throws SQLException {
        logger.debug("getCharacter(id={}) called", id);
        Character character = db.getCharacter(id).orElse(null);
        if (character != null) {
            loadCharacterDetails(character);
            logger.debug("Character found: id={}, name={}", character.getId(), character.getName());
        } else {
            logger.debug("Character with id={} not found", id);
        }
        return character;
    }

    /**
     * Обновляет дату последнего открытия персонажа на текущую и сохраняет изменения.
     * @param character объект персонажа (должен содержать корректный ID)
     * @return тот же объект с обновлённой датой
     */
    public Character updateLastOpened(Character character) throws SQLException {
        logger.debug("updateLastOpened(character id={}) called", character.getId());
        character.setLastOpened(LocalDateTime.now());
        db.updateCharacter(character);
        logger.info("Last opened date updated for character id={}", character.getId());
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
        logger.info("Updating character id={}, name={}", character.getId(), character.getName());

        // 1. Обновляем основные поля
        db.updateCharacter(character);
        logger.debug("Basic fields updated for character id={}", character.getId());

        // 2. Синхронизируем теги
        synchronizeTags(character);

        // 3. Синхронизируем пакеты
        synchronizePackages(character);

        // 4. Синхронизируем бэкапы
        synchronizeBackups(character);

        // Перезагружаем полные данные, чтобы получить актуальные списки (включая ID новых бэкапов)
        loadCharacterDetails(character);
        logger.info("Character id={} updated successfully", character.getId());
        return character;
    }

    /**
     * Добавляет нового персонажа в базу. Устанавливает сгенерированный ID в переданный объект.
     * После добавления также сохраняются все теги и пакеты, указанные в объекте.
     * @param character объект нового персонажа (ID игнорируется)
     * @return тот же объект с заполненным ID
     */
    public Character addCharacter(Character character) throws SQLException {
        logger.info("Adding new character with name: {}", character.getName());

        // Сохраняем основную запись
        int id = db.addCharacter(character);
        character.setId(id);
        logger.debug("Character inserted with generated id={}", id);

        // Добавляем теги и пакеты
        if (character.getTags() != null) {
            logger.debug("Adding {} tags for new character", character.getTags().size());
            for (Tag tag : character.getTags()) {
                db.addTagToCharacter(id, tag.getName(), tag.getColor());
            }
        }
        if (character.getPackages() != null) {
            logger.debug("Adding {} packages for new character", character.getPackages().size());
            for (Package pkg : character.getPackages()) {
                db.addPackageToCharacter(id, pkg.getId());
            }
        }

        loadCharacterDetails(character);
        logger.info("Character added successfully: id={}, name={}", character.getId(), character.getName());
        return character;
    }

    /**
     * Удаляет персонажа вместе со всеми связями (теги, пакеты, бэкапы).
     * Сами теги и пакеты не удаляются.
     * @param character объект персонажа (достаточно наличия ID)
     */
    public void deleteCharacter(Character character) throws SQLException {
        logger.info("Deleting character id={}, name={}", character.getId(), character.getName());
        db.deleteCharacter(character.getId());
        logger.debug("Character id={} deleted", character.getId());
    }

    // ==================== Теги ====================

    /**
     * Возвращает список всех тегов.
     */
    public List<Tag> getAllTags() throws SQLException {
        logger.debug("getAllTags() called");
        List<Tag> tags = db.getAllTags();
        logger.debug("Returning {} tags", tags.size());
        return tags;
    }

    /**
     * Добавляет новый тег. Если тег с таким именем уже существует,
     * возвращает существующий (ID будет установлен в переданный объект).
     * @param tag объект тега (имя должно быть задано, ID игнорируется)
     * @return тот же объект с установленным ID
     */
    public Tag addTag(Tag tag) throws SQLException {
        logger.info("Adding tag with name: {}", tag.getName());
        int id = db.addTag(tag); // addTag возвращает ID существующего или нового
        tag.setId(id);
        logger.info("Tag added/retrieved with id={}", id);
        return tag;
    }

    // ==================== Пакеты ====================

    /**
     * Возвращает список всех пакетов.
     */
    public List<Package> getAllPackages() throws SQLException {
        logger.debug("getAllPackages() called");
        List<Package> packages = db.getAllPackages();
        logger.debug("Returning {} packages", packages.size());
        return packages;
    }

    /**
     * Добавляет новый пакет.
     * @param pkg объект пакета (все поля, кроме ID, должны быть заполнены)
     * @return тот же объект с установленным ID
     */
    public Package addPackage(Package pkg) throws SQLException {
        logger.info("Adding package: name={}, description={}", pkg.getName(), pkg.getDescription());
        int id = db.addPackage(pkg);
        pkg.setId(id);
        logger.info("Package added with id={}", id);
        return pkg;
    }

    /**
     * Удаляет пакет. Если пакет используется хотя бы одним персонажем,
     * выбрасывает исключение PackageInUseException.
     * @param pkg объект пакета (должен содержать ID)
     * @throws PackageInUseException если пакет привязан к персонажам
     */
    public void deletePackage(Package pkg) throws SQLException, PackageInUseException {
        logger.info("Deleting package id={}, name={}", pkg.getId(), pkg.getName());
        List<Character> characters = db.getCharactersForPackage(pkg.getId());
        if (!characters.isEmpty()) {
            logger.warn("Cannot delete package id={}: used by {} characters", pkg.getId(), characters.size());
            throw new PackageInUseException("Пакет \"" + pkg.getName() + "\" используется персонажами и не может быть удалён.");
        }
        db.deletePackage(pkg.getId());
        logger.info("Package id={} deleted successfully", pkg.getId());
    }

    // ==================== Бэкапы персонажей ====================

    /**
     * Возвращает список бэкапов для указанного персонажа.
     */
    public List<Backup> getBackupsForCharacter(int characterId) throws SQLException {
        logger.debug("getBackupsForCharacter(characterId={}) called", characterId);
        List<Backup> backups = db.getBackupsForCharacter(characterId);
        logger.debug("Found {} backups for character id={}", backups.size(), characterId);
        return backups;
    }

    /**
     * Добавляет новый бэкап для персонажа.
     * @param backup объект бэкапа (characterId должен быть установлен, id игнорируется)
     * @return тот же объект с установленным id
     */
    public Backup addBackup(Backup backup) throws SQLException {
        logger.info("Adding backup for character id={}, path={}", backup.getCharacterId(), backup.getBackupPath());
        int id = db.addBackup(backup);
        backup.setId(id);
        logger.info("Backup added with id={}", id);
        return backup;
    }

    /**
     * Удаляет бэкап.
     */
    public void deleteBackup(Backup backup) throws SQLException {
        logger.info("Deleting backup id={} for character id={}", backup.getId(), backup.getCharacterId());
        db.deleteBackup(backup.getId());
        logger.debug("Backup id={} deleted", backup.getId());
    }

    /**
     * Удаляет бэкап по его идентификатору.
     */
    public void deleteBackup(int backupId) throws SQLException {
        logger.info("Deleting backup id={}", backupId);
        db.deleteBackup(backupId);
        logger.debug("Backup id={} deleted", backupId);
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Загружает в объект персонажа связанные теги, бэкапы и пакеты.
     */
    private void loadCharacterDetails(Character character) throws SQLException {
        int id = character.getId();
        logger.trace("Loading details for character id={}", id);
        character.setTags(db.getTagObjectsForCharacter(id));
        character.setBackups(db.getBackupsForCharacter(id));
        character.setPackages(db.getPackagesForCharacter(id));
        logger.trace("Details loaded for character id={}: {} tags, {} backups, {} packages",
                id, character.getTags().size(), character.getBackups().size(), character.getPackages().size());
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
        if (newTagNames == null) {
            newTagNames = Collections.emptyList();
        }

        logger.debug("Synchronizing tags for character id={}: current {} tags, new {} tags",
                charId, currentTagNames.size(), newTagNames.size());

        // Удаляем лишние
        int removed = 0;
        for (Tag tag : currentTagNames) {
            if (!newTagNames.contains(tag)) {
                db.removeTagFromCharacter(charId, tag.getName());
                removed++;
            }
        }
        // Добавляем новые
        int added = 0;
        for (Tag tag : newTagNames) {
            if (!currentTagNames.contains(tag)) {
                db.addTagToCharacter(charId, tag.getName(), tag.getColor());
                added++;
            }
        }
        if (removed > 0 || added > 0) {
            logger.info("Tags synchronized for character id={}: removed {}, added {}", charId, removed, added);
        } else {
            logger.debug("No tag changes for character id={}", charId);
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

        logger.debug("Synchronizing backups for character id={}: current {} backups, new {} backups",
                charId, currentBackups.size(), newBackups.size());

        // Составляем множества id для быстрого поиска
        Set<Integer> currentIds = currentBackups.stream().map(Backup::getId).collect(Collectors.toSet());
        Set<Integer> newIds = newBackups.stream().map(Backup::getId).filter(id -> id != 0).collect(Collectors.toSet());

        // 1. Удаляем те, что есть в БД, но отсутствуют в новом списке
        int deleted = 0;
        for (Integer id : currentIds) {
            if (!newIds.contains(id)) {
                db.deleteBackup(id);
                deleted++;
            }
        }

        // 2. Обрабатываем каждый бэкап из нового списка
        int inserted = 0, updated = 0;
        for (Backup backup : newBackups) {
            backup.setCharacterId(charId); // гарантируем правильный characterId
            if (backup.getId() == 0) {
                // Новый бэкап
                int newId = db.addBackup(backup);
                backup.setId(newId);
                inserted++;
            } else {
                // Существующий бэкап – обновляем
                db.updateBackup(backup);
                updated++;
            }
        }

        if (deleted > 0 || inserted > 0 || updated > 0) {
            logger.info("Backups synchronized for character id={}: deleted {}, inserted {}, updated {}",
                    charId, deleted, inserted, updated);
        } else {
            logger.debug("No backup changes for character id={}", charId);
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

        logger.debug("Synchronizing packages for character id={}: current {} packages, new {} packages",
                charId, currentPackages.size(), newPackages.size());

        // Удаляем лишние
        int removed = 0;
        for (Integer id : currentIds) {
            if (!newIds.contains(id)) {
                db.removePackageFromCharacter(charId, id);
                removed++;
            }
        }
        // Добавляем новые
        int added = 0;
        for (Integer id : newIds) {
            if (!currentIds.contains(id)) {
                db.addPackageToCharacter(charId, id);
                added++;
            }
        }
        if (removed > 0 || added > 0) {
            logger.info("Packages synchronized for character id={}: removed {}, added {}", charId, removed, added);
        } else {
            logger.debug("No package changes for character id={}", charId);
        }
    }
}