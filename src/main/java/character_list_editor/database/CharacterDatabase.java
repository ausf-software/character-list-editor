package character_list_editor.database;

import character_list_editor.utils.PathUtil;

import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class CharacterDatabase {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String dbUrl;
    private Connection connection;

    private final static String FILE_NAME = "character_data.db";

    public CharacterDatabase() {
        this.dbUrl = "jdbc:sqlite:" + PathUtil.APP_DIR + FILE_NAME;
    }

    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void createTables() throws SQLException {
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
        }
    }

    // ==================== Методы для работы с персонажами ====================

    public int addCharacter(Character character) throws SQLException {
        String sql = "INSERT INTO characters (name, campaign, last_opened, sheet_path) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, character.getName());
            pstmt.setString(2, character.getCampaign());
            pstmt.setString(3, character.getLastOpened().format(DATE_FORMAT));
            pstmt.setString(4, character.getSheetPath());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Не удалось получить сгенерированный ID");
            }
        }
    }

    public Optional<Character> getCharacter(int id) throws SQLException {
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
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public void updateCharacter(Character character) throws SQLException {
        String sql = "UPDATE characters SET name = ?, campaign = ?, last_opened = ?, sheet_path = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, character.getName());
            pstmt.setString(2, character.getCampaign());
            pstmt.setString(3, character.getLastOpened().format(DATE_FORMAT));
            pstmt.setString(4, character.getSheetPath());
            pstmt.setInt(5, character.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteCharacter(int id) throws SQLException {
        String sql = "DELETE FROM characters WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<Character> getAllCharacters() throws SQLException {
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
        }
        return list;
    }

    // ==================== Методы для работы с пакетами ====================

    public List<Package> getAllPackages() throws SQLException {
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
        }
        return packages;
    }

    public int addPackage(Package pkg) throws SQLException {
        String sql = "INSERT INTO packages (name, description, file_path, version) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, pkg.getName());
            pstmt.setString(2, pkg.getDescription());
            pstmt.setString(3, pkg.getFilePath());
            pstmt.setString(4, pkg.getVersion());
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            else throw new SQLException("Не удалось получить ID пакета");
        }
    }

    public Optional<Package> getPackage(int id) throws SQLException {
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
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public void updatePackage(Package pkg) throws SQLException {
        String sql = "UPDATE packages SET name = ?, description = ?, file_path = ?, version = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pkg.getName());
            pstmt.setString(2, pkg.getDescription());
            pstmt.setString(3, pkg.getFilePath());
            pstmt.setString(4, pkg.getVersion());
            pstmt.setInt(5, pkg.getId());
            pstmt.executeUpdate();
        }
    }

    public void deletePackage(int id) throws SQLException {
        String sql = "DELETE FROM packages WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    // ==================== Методы для связи персонажей и пакетов ====================

    public void addPackageToCharacter(int characterId, int packageId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO character_package (character_id, package_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            pstmt.setInt(2, packageId);
            pstmt.executeUpdate();
        }
    }

    public void removePackageFromCharacter(int characterId, int packageId) throws SQLException {
        String sql = "DELETE FROM character_package WHERE character_id = ? AND package_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, characterId);
            pstmt.setInt(2, packageId);
            pstmt.executeUpdate();
        }
    }

    public List<Package> getPackagesForCharacter(int characterId) throws SQLException {
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
        }
        return packages;
    }

    public List<Character> getCharactersForPackage(int packageId) throws SQLException {
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
        }
        return characters;
    }

    // ==================== Методы для работы с тегами ====================

    private OptionalInt getTagIdByName(String tagName) throws SQLException {
        String sql = "SELECT id FROM tags WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tagName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return OptionalInt.of(rs.getInt("id"));
            }
        }
        return OptionalInt.empty();
    }

    public int createTag(Tag tag) throws SQLException {
        String sql = "INSERT INTO tags (name, color) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, tag.getName());
            pstmt.setInt(2, tag.getColorRGB());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Не удалось создать тег");
            }
        }
    }

    /**
     * Добавляет тег к персонажу. Если тег с таким именем не существует, он создаётся.
     */
    public void addTagToCharacter(int characterId, String tagName, Color backgroundColor) throws SQLException {
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
            pstmt.executeUpdate();
        }
    }

    /**
     * Удаляет тег у персонажа.
     */
    public void removeTagFromCharacter(int characterId, String tagName) throws SQLException {
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
                    dpstmt.executeUpdate();
                }
            }
        }
    }

    /**
     * Возвращает список имён тегов для персонажа.
     */
    public List<String> getTagsForCharacter(int characterId) throws SQLException {
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
        }
        return tags;
    }

    /**
     * Возвращает список объектов Tag для персонажа.
     */
    public List<Tag> getTagObjectsForCharacter(int characterId) throws SQLException {
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
        }
        return tags;
    }

    /**
     * Возвращает все теги из таблицы tags.
     */
    public List<Tag> getAllTags() throws SQLException {
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
        }
        return tags;
    }

    /**
     * Добавляет новый тег в таблицу tags. Если тег с таким именем уже существует, возвращает его ID.
     */
    public int addTag(Tag tag) throws SQLException {
        // Проверяем, существует ли уже тег с таким именем
        OptionalInt optId = getTagIdByName(tag.getName());
        if (optId.isPresent()) {
            return optId.getAsInt();
        } else {
            return createTag(tag);
        }
    }

    /**
     * Удаляет тег из таблицы tags. При наличии связей с персонажами они будут удалены благодаря ON DELETE CASCADE.
     */
    public void deleteTag(int tagId) throws SQLException {
        String sql = "DELETE FROM tags WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, tagId);
            pstmt.executeUpdate();
        }
    }

    // ==================== Методы для бэкапов персонажей ====================

    public int addBackup(Backup backup) throws SQLException {
        String sql = "INSERT INTO character_backups (character_id, backup_path, backup_date) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, backup.getCharacterId());
            pstmt.setString(2, backup.getBackupPath());
            pstmt.setString(3, backup.getBackupDate().format(DATE_FORMAT));
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            else throw new SQLException("Не удалось получить ID бэкапа");
        }
    }

    public List<Backup> getBackupsForCharacter(int characterId) throws SQLException {
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
        }
        return backups;
    }

    public void updateBackup(Backup backup) throws SQLException {
        String sql = "UPDATE character_backups SET backup_path = ?, backup_date = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, backup.getBackupPath());
            pstmt.setString(2, backup.getBackupDate().format(DATE_FORMAT));
            pstmt.setInt(3, backup.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteBackup(int backupId) throws SQLException {
        String sql = "DELETE FROM character_backups WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, backupId);
            pstmt.executeUpdate();
        }
    }
}