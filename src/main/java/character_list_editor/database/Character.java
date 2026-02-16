package character_list_editor.database;

import java.time.LocalDateTime;
import java.util.List;

public class Character {
    private int id;
    private String name;
    private String campaign;
    private LocalDateTime lastOpened;
    private String sheetPath;
    private List<String> tags;
    private List<Backup> backups;

    public Character() {}

    public Character(String name, String campaign, LocalDateTime lastOpened, String sheetPath) {
        this.name = name;
        this.campaign = campaign;
        this.lastOpened = lastOpened;
        this.sheetPath = sheetPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    public LocalDateTime getLastOpened() {
        return lastOpened;
    }

    public void setLastOpened(LocalDateTime lastOpened) {
        this.lastOpened = lastOpened;
    }

    public String getSheetPath() {
        return sheetPath;
    }

    public void setSheetPath(String sheetPath) {
        this.sheetPath = sheetPath;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Backup> getBackups() {
        return backups;
    }

    public void setBackups(List<Backup> backups) {
        this.backups = backups;
    }
}
