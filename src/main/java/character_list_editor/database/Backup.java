package character_list_editor.database;

import java.time.LocalDateTime;

public class Backup {
    private int id;
    private int characterId;
    private String backupPath;
    private LocalDateTime backupDate;

    public Backup(int id, int characterId, String backupPath, LocalDateTime backupDate) {
        this.id = id;
        this.characterId = characterId;
        this.backupPath = backupPath;
        this.backupDate = backupDate;
    }

    public Backup() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public LocalDateTime getBackupDate() {
        return backupDate;
    }

    public void setBackupDate(LocalDateTime backupDate) {
        this.backupDate = backupDate;
    }
}