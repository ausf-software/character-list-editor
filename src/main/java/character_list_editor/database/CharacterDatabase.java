package character_list_editor.database;

import character_list_editor.utils.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class CharacterDatabase {
    private static final Logger logger = LoggerFactory.getLogger(CharacterDatabase.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FILE_NAME = "character_data.db";

    private final String dbUrl;
    private Connection connection;

    public CharacterDatabase() {
        this.dbUrl = "jdbc:sqlite:" + PathUtil.APP_DIR + FILE_NAME;
        logger.debug("Database URL: {}", dbUrl);
    }

    /**
     * Ensures that the database connection is open and valid.
     * If not, calls connect().
     */
    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            logger.debug("Database connection is closed or null, attempting to connect...");
            connect();
        }
    }

    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            logger.info("Connecting to database: {}", dbUrl);
            connection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
            logger.info("Database connected successfully");
        } else {
            logger.debug("Connection already established");
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                logger.info("Disconnecting from database");
                connection.close();
                logger.info("Database disconnected");
            } catch (SQLException e) {
                logger.error("Error while disconnecting from database", e);
            }
        } else {
            logger.debug("No active connection to close");
        }
    }

    public void createTables() throws SQLException {
        ensureConnection();
        logger.info("Creating tables if they do not exist");

        String sql =
                "CREATE TABLE IF NOT EXISTS characters (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    name TEXT NOT NULL," +
                        "    campaign TEXT NOT NULL," +
                        "    last_opened TEXT NOT NULL," +
                        "    sheet_path TEXT NOT NULL" +
                        ");" +

                        "CREATE TABLE IF NOT EXISTS packages (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    name TEXT NOT NULL UNIQUE," +
                        "    description TEXT," +
                        "    file_path TEXT NOT NULL," +
                        "    version TEXT" +
                        ");" +

                        "CREATE TABLE IF NOT EXISTS character_package (" +
                        "    character_id INTEGER NOT NULL," +
                        "    package_id INTEGER NOT NULL," +
                        "    PRIMARY KEY (character_id, package_id)," +
                        "    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE," +
                        "    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE CASCADE" +
                        ");" +

                        "CREATE TABLE IF NOT EXISTS tags (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    name TEXT NOT NULL UNIQUE," +
                        "    color INTEGER" +
                        ");" +

                        "CREATE TABLE IF NOT EXISTS character_tags (" +
                        "    character_id INTEGER NOT NULL," +
                        "    tag_id INTEGER NOT NULL," +
                        "    PRIMARY KEY (character_id, tag_id)," +
                        "    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE," +
                        "    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE" +
                        ");" +

                        "CREATE TABLE IF NOT EXISTS character_backups (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "    character_id INTEGER NOT NULL," +
                        "    backup_path TEXT NOT NULL," +
                        "    backup_date TEXT NOT NULL," +
                        "    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE" +
                        ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Tables created/verified successfully");
        } catch (SQLException e) {
            logger.error("Failed to create tables", e);
            throw e;
        }
    }

    // ==================== Методы для работы с персонажами ====================

    public int addCharacter(Character character) throws SQLException {
        ensureConnection();
        logger.debug("Adding character: name={}, campaign={}, sheetPath={}",
                character.getName(), character.getCampaign(), character.getSheetPath());

        String sql = "INSERT INTO characters (name, campaign, last_opened, sheet_path) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, character.getName());
            pstmt.setString(2, character.getCampaign());
            pstmt.setString(3, character.getLastOpened().format(DATE_FORMAT));
            pstmt.setString(4, character.getSheetPath());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.debug("Character added with ID: {}", id);
                return id;
            } else {
                logger.error("No generated key returned for new character");
                throw new SQLException("Не удалось получить сгенерированный ID");
            }
        } catch (SQLException e) {
            logger.error("Error adding character", e);
            throw e;
        }
    }

    public Optional<Character> getCharacter(int id) throws SQLException {
        ensureConnection();
        logger.debug("Fetching character with ID: {}", id);

        String sql = "SELECT id, name, campaign, last_opened, sheet_path FROM characters WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Character c = new Character();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setCampaign(rs.getString("campaign"));
                c.setLastOpened(LocalDateTime.parse(rs.getString("last_opened"), DATE_FORMAT));
                c.setSheetPath(rs.getString("sheet_path"));
                logger.debug("Character found: {}", c.getName());
                return Optional.of(c);
            } else {
                logger.debug("No character found with ID: {}", id);
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Error fetching character with ID: {}", id, e);
            throw e;
        }
    }

    public void updateCharacter(Character character) throws SQLException {
        ensureConnection();
        logger.debug("Updating character ID: {}", character.getId());

        String sql = "UPDATE characters SET name = ?, campaign = ?, last_opened = ?, sheet_path = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, character.getName());
            pstmt.setString(2, character.getCampaign());
            pstmt.setString(3, character.getLastOpened().format(DATE_FORMAT));
            pstmt.setString(4, character.getSheetPath());
            pstmt.setInt(5, character.getId());
            int rows = pstmt.executeUpdate();
            logger.debug("Character updated, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error updating character ID: {}", character.getId(), e);
            throw e;
        }
    }

    public void deleteCharacter(int id) throws SQLException {
        ensureConnection();
        logger.debug("Deleting character with ID: {}", id);

        String sql = "DELETE FROM characters WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            logger.debug("Character deleted, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error deleting character ID: {}", id, e);
            throw e;
        }
    }

    public List<Character> getAllCharacters() throws SQLException {
        ensureConnection();
        logger.debug("Fetching all characters");

        List<Character> list = new ArrayList<>();
        String sql = "SELECT id, name, campaign, last_opened, sheet_path FROM characters ORDER BY last_opened DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Character c = new Character();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setCampaign(rs.getString("campaign"));
                c.setLastOpened(LocalDateTime.parse(rs.getString("last_opened"), DATE_FORMAT));
                c.setSheetPath(rs.getString("sheet_path"));
                list.add(c);
            }
        } catch (SQLException e) {
            logger.error("Error fetching all characters", e);
            throw e;
        }
        logger.debug("Fetched {} characters", list.size());
        return list;
    }

    // ==================== Методы для работы с пакетами ====================

    public List<Package> getAllPackages() throws SQLException {
        ensureConnection();
        logger.debug("Fetching all packages");

        List<Package> packages = new ArrayList<>();
        String sql = "SELECT id, name, description, file_path, version FROM packages ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Package p = new Package();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setFilePath(rs.getString("file_path"));
                p.setVersion(rs.getString("version"));
                packages.add(p);
            }
        } catch (SQLException e) {
            logger.error("Error fetching all packages", e);
            throw e;
        }
        logger.debug("Fetched {} packages", packages.size());
        return packages;
    }

    public int addPackage(Package pkg) throws SQLException {
        ensureConnection();
        logger.debug("Adding package: name={}, filePath={}", pkg.getName(), pkg.getFilePath());

        String sql = "INSERT INTO packages (name, description, file_path, version) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, pkg.getName());
            pstmt.setString(2, pkg.getDescription());
            pstmt.setString(3, pkg.getFilePath());
            pstmt.setString(4, pkg.getVersion());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.debug("Package added with ID: {}", id);
                return id;
            } else {
                logger.error("No generated key returned for new package");
                throw new SQLException("Не удалось получить ID пакета");
            }
        } catch (SQLException e) {
            logger.error("Error adding package", e);
            throw e;
        }
    }

    public Optional<Package> getPackage(int id) throws SQLException {
        ensureConnection();
        logger.debug("Fetching package with ID: {}", id);

        String sql = "SELECT id, name, description, file_path, version FROM packages WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Package p = new Package();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setFilePath(rs.getString("file_path"));
                p.setVersion(rs.getString("version"));
                logger.debug("Package found: {}", p.getName());
                return Optional.of(p);
            } else {
                logger.debug("No package found with ID: {}", id);
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.error("Error fetching package ID: {}", id, e);
            throw e;
        }
    }

    public void updatePackage(Package pkg) throws SQLException {
        ensureConnection();
        logger.debug("Updating package ID: {}", pkg.getId());

        String sql = "UPDATE packages SET name = ?, description = ?, file_path = ?, version = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pkg.getName());
            pstmt.setString(2, pkg.getDescription());
            pstmt.setString(3, pkg.getFilePath());
            pstmt.setString(4, pkg.getVersion());
            pstmt.setInt(5, pkg.getId());
            int rows = pstmt.executeUpdate();
            logger.debug("Package updated, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error updating package ID: {}", pkg.getId(), e);
            throw e;
        }
    }

    public void deletePackage(int id) throws SQLException {
        ensureConnection();
        logger.debug("Deleting package with ID: {}", id);

        String sql = "DELETE FROM packages WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            logger.debug("Package deleted, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error deleting package ID: {}", id, e);
            throw e;
        }
    }

    // ==================== Методы для связи персонажей и пакетов ====================

    public void addPackageToCharacter(int characterId, int packageId) throws SQLException {
        ensureConnection();
        logger.debug("Adding package ID {} to character ID {}", packageId, characterId);

        String sql = "INSERT OR IGNORE INTO character_package (character_id, package_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            pstmt.setInt(2, packageId);
            int rows = pstmt.executeUpdate();
            logger.debug("Package added to character, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error adding package {} to character {}", packageId, characterId, e);
            throw e;
        }
    }

    public void removePackageFromCharacter(int characterId, int packageId) throws SQLException {
        ensureConnection();
        logger.debug("Removing package ID {} from character ID {}", packageId, characterId);

        String sql = "DELETE FROM character_package WHERE character_id = ? AND package_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            pstmt.setInt(2, packageId);
            int rows = pstmt.executeUpdate();
            logger.debug("Package removed from character, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error removing package {} from character {}", packageId, characterId, e);
            throw e;
        }
    }

    public List<Package> getPackagesForCharacter(int characterId) throws SQLException {
        ensureConnection();
        logger.debug("Fetching packages for character ID: {}", characterId);

        List<Package> packages = new ArrayList<>();
        String sql = "SELECT p.id, p.name, p.description, p.file_path, p.version " +
                "FROM packages p " +
                "JOIN character_package cp ON p.id = cp.package_id " +
                "WHERE cp.character_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Package p = new Package();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setFilePath(rs.getString("file_path"));
                p.setVersion(rs.getString("version"));
                packages.add(p);
            }
        } catch (SQLException e) {
            logger.error("Error fetching packages for character {}", characterId, e);
            throw e;
        }
        logger.debug("Found {} packages for character {}", packages.size(), characterId);
        return packages;
    }

    public List<Character> getCharactersForPackage(int packageId) throws SQLException {
        ensureConnection();
        logger.debug("Fetching characters for package ID: {}", packageId);

        List<Character> characters = new ArrayList<>();
        String sql = "SELECT c.id, c.name, c.campaign, c.last_opened, c.sheet_path " +
                "FROM characters c " +
                "JOIN character_package cp ON c.id = cp.character_id " +
                "WHERE cp.package_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, packageId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Character c = new Character();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setCampaign(rs.getString("campaign"));
                c.setLastOpened(LocalDateTime.parse(rs.getString("last_opened"), DATE_FORMAT));
                c.setSheetPath(rs.getString("sheet_path"));
                characters.add(c);
            }
        } catch (SQLException e) {
            logger.error("Error fetching characters for package {}", packageId, e);
            throw e;
        }
        logger.debug("Found {} characters for package {}", characters.size(), packageId);
        return characters;
    }

    // ==================== Методы для работы с тегами ====================

    private OptionalInt getTagIdByName(String tagName) throws SQLException {
        ensureConnection(); // хотя приватный, но на всякий случай
        logger.trace("Getting tag ID for name: {}", tagName);

        String sql = "SELECT id FROM tags WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tagName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                logger.trace("Tag ID {} found for name '{}'", id, tagName);
                return OptionalInt.of(id);
            }
        } catch (SQLException e) {
            logger.error("Error getting tag ID for name '{}'", tagName, e);
            throw e;
        }
        logger.trace("No tag found with name '{}'", tagName);
        return OptionalInt.empty();
    }

    public int createTag(Tag tag) throws SQLException {
        ensureConnection();
        logger.debug("Creating new tag: name={}, color={}", tag.getName(), tag.getColorRGB());

        String sql = "INSERT INTO tags (name, color) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, tag.getName());
            pstmt.setInt(2, tag.getColorRGB());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.debug("Tag created with ID: {}", id);
                return id;
            } else {
                logger.error("No generated key returned for new tag");
                throw new SQLException("Не удалось создать тег");
            }
        } catch (SQLException e) {
            logger.error("Error creating tag", e);
            throw e;
        }
    }

    /**
     * Добавляет тег к персонажу. Если тег с таким именем не существует, он создаётся.
     */
    public void addTagToCharacter(int characterId, String tagName, Color backgroundColor) throws SQLException {
        ensureConnection();
        logger.debug("Adding tag '{}' to character ID {}", tagName, characterId);

        // Пытаемся получить ID существующего тега
        OptionalInt optId = getTagIdByName(tagName);
        int tagId;
        if (optId.isPresent()) {
            tagId = optId.getAsInt();
        } else {
            Tag newTag = new Tag();
            newTag.setName(tagName);
            newTag.setColor(backgroundColor);
            tagId = createTag(newTag);
        }
        // Добавляем связь
        String sql = "INSERT OR IGNORE INTO character_tags (character_id, tag_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            pstmt.setInt(2, tagId);
            int rows = pstmt.executeUpdate();
            logger.debug("Tag added to character, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error adding tag '{}' to character {}", tagName, characterId, e);
            throw e;
        }
    }

    /**
     * Удаляет тег у персонажа.
     */
    public void removeTagFromCharacter(int characterId, String tagName) throws SQLException {
        ensureConnection();
        logger.debug("Removing tag '{}' from character ID {}", tagName, characterId);

        // Сначала получаем ID тега (если его нет, то и удалять нечего)
        String selectSql = "SELECT id FROM tags WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setString(1, tagName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int tagId = rs.getInt("id");
                String deleteSql = "DELETE FROM character_tags WHERE character_id = ? AND tag_id = ?";
                try (PreparedStatement dpstmt = connection.prepareStatement(deleteSql)) {
                    dpstmt.setInt(1, characterId);
                    dpstmt.setInt(2, tagId);
                    int rows = dpstmt.executeUpdate();
                    logger.debug("Tag removed from character, rows affected: {}", rows);
                }
            } else {
                logger.debug("Tag '{}' not found, nothing to remove", tagName);
            }
        } catch (SQLException e) {
            logger.error("Error removing tag '{}' from character {}", tagName, characterId, e);
            throw e;
        }
    }

    /**
     * Возвращает список имён тегов для персонажа.
     */
    public List<String> getTagsForCharacter(int characterId) throws SQLException {
        ensureConnection();
        logger.debug("Fetching tag names for character ID: {}", characterId);

        List<String> tags = new ArrayList<>();
        String sql = "SELECT t.name " +
                "FROM tags t " +
                "JOIN character_tags ct ON t.id = ct.tag_id " +
                "WHERE ct.character_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tags.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            logger.error("Error fetching tag names for character {}", characterId, e);
            throw e;
        }
        logger.debug("Found {} tags for character {}", tags.size(), characterId);
        return tags;
    }

    /**
     * Возвращает список объектов Tag для персонажа.
     */
    public List<Tag> getTagObjectsForCharacter(int characterId) throws SQLException {
        ensureConnection();
        logger.debug("Fetching Tag objects for character ID: {}", characterId);

        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT t.id, t.name, t.color " +
                "FROM tags t " +
                "JOIN character_tags ct ON t.id = ct.tag_id " +
                "WHERE ct.character_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColorRGB(rs.getInt("color"));
                tags.add(tag);
            }
        } catch (SQLException e) {
            logger.error("Error fetching Tag objects for character {}", characterId, e);
            throw e;
        }
        logger.debug("Found {} Tag objects for character {}", tags.size(), characterId);
        return tags;
    }

    /**
     * Возвращает все теги из таблицы tags.
     */
    public List<Tag> getAllTags() throws SQLException {
        ensureConnection();
        logger.debug("Fetching all tags");

        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT id, name, color FROM tags ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Tag tag = new Tag();
                tag.setId(rs.getInt("id"));
                tag.setName(rs.getString("name"));
                tag.setColorRGB(rs.getInt("color"));
                tags.add(tag);
            }
        } catch (SQLException e) {
            logger.error("Error fetching all tags", e);
            throw e;
        }
        logger.debug("Fetched {} tags", tags.size());
        return tags;
    }

    /**
     * Добавляет новый тег в таблицу tags. Если тег с таким именем уже существует, возвращает его ID.
     */
    public int addTag(Tag tag) throws SQLException {
        ensureConnection();
        logger.debug("Adding tag (if not exists): name={}", tag.getName());

        // Проверяем, существует ли уже тег с таким именем
        OptionalInt optId = getTagIdByName(tag.getName());
        if (optId.isPresent()) {
            logger.debug("Tag already exists with ID: {}", optId.getAsInt());
            return optId.getAsInt();
        } else {
            return createTag(tag);
        }
    }

    /**
     * Удаляет тег из таблицы tags. При наличии связей с персонажами они будут удалены благодаря ON DELETE CASCADE.
     */
    public void deleteTag(int tagId) throws SQLException {
        ensureConnection();
        logger.debug("Deleting tag with ID: {}", tagId);

        String sql = "DELETE FROM tags WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, tagId);
            int rows = pstmt.executeUpdate();
            logger.debug("Tag deleted, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error deleting tag ID: {}", tagId, e);
            throw e;
        }
    }

    // ==================== Методы для бэкапов персонажей ====================

    public int addBackup(Backup backup) throws SQLException {
        ensureConnection();
        logger.debug("Adding backup for character ID: {}, path: {}", backup.getCharacterId(), backup.getBackupPath());

        String sql = "INSERT INTO character_backups (character_id, backup_path, backup_date) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, backup.getCharacterId());
            pstmt.setString(2, backup.getBackupPath());
            pstmt.setString(3, backup.getBackupDate().format(DATE_FORMAT));
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.debug("Backup added with ID: {}", id);
                return id;
            } else {
                logger.error("No generated key returned for new backup");
                throw new SQLException("Не удалось получить ID бэкапа");
            }
        } catch (SQLException e) {
            logger.error("Error adding backup", e);
            throw e;
        }
    }

    public List<Backup> getBackupsForCharacter(int characterId) throws SQLException {
        ensureConnection();
        logger.debug("Fetching backups for character ID: {}", characterId);

        List<Backup> backups = new ArrayList<>();
        String sql = "SELECT id, character_id, backup_path, backup_date FROM character_backups WHERE character_id = ? ORDER BY backup_date DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Backup b = new Backup();
                b.setId(rs.getInt("id"));
                b.setCharacterId(rs.getInt("character_id"));
                b.setBackupPath(rs.getString("backup_path"));
                b.setBackupDate(LocalDateTime.parse(rs.getString("backup_date"), DATE_FORMAT));
                backups.add(b);
            }
        } catch (SQLException e) {
            logger.error("Error fetching backups for character {}", characterId, e);
            throw e;
        }
        logger.debug("Found {} backups for character {}", backups.size(), characterId);
        return backups;
    }

    public void updateBackup(Backup backup) throws SQLException {
        ensureConnection();
        logger.debug("Updating backup ID: {}", backup.getId());

        String sql = "UPDATE character_backups SET backup_path = ?, backup_date = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, backup.getBackupPath());
            pstmt.setString(2, backup.getBackupDate().format(DATE_FORMAT));
            pstmt.setInt(3, backup.getId());
            int rows = pstmt.executeUpdate();
            logger.debug("Backup updated, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error updating backup ID: {}", backup.getId(), e);
            throw e;
        }
    }

    public void deleteBackup(int backupId) throws SQLException {
        ensureConnection();
        logger.debug("Deleting backup with ID: {}", backupId);

        String sql = "DELETE FROM character_backups WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, backupId);
            int rows = pstmt.executeUpdate();
            logger.debug("Backup deleted, rows affected: {}", rows);
        } catch (SQLException e) {
            logger.error("Error deleting backup ID: {}", backupId, e);
            throw e;
        }
    }
}