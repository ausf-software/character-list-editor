-- Включение поддержки внешних ключей
PRAGMA foreign_keys = ON;

-- Таблица персонажей
CREATE TABLE IF NOT EXISTS characters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    campaign TEXT NOT NULL,
    last_opened TEXT NOT NULL, -- дата в формате ISO (YYYY-MM-DD HH:MM:SS)
    sheet_path TEXT NOT NULL
);

-- Таблица пакетов
CREATE TABLE IF NOT EXISTS packages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    file_path TEXT NOT NULL,
    version TEXT
);

-- Связь многие-ко-многим: персонажи и пакеты
CREATE TABLE IF NOT EXISTS character_package (
    character_id INTEGER NOT NULL,
    package_id INTEGER NOT NULL,
    PRIMARY KEY (character_id, package_id),
    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE CASCADE
);

-- Таблица тегов (уникальные названия)
CREATE TABLE IF NOT EXISTS tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

-- Связь многие-ко-многим: персонажи и теги
CREATE TABLE IF NOT EXISTS character_tags (
    character_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (character_id, tag_id),
    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Бэкапы персонажей
CREATE TABLE IF NOT EXISTS character_backups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL,
    backup_path TEXT NOT NULL,
    backup_date TEXT NOT NULL, -- дата в формате ISO
    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE
);