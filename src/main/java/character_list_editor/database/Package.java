package character_list_editor.database;

public class Package {
    private int id;
    private String name;
    private String description;
    private String filePath;
    private String version;

    public Package(int id, String name, String description, String filePath, String version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.filePath = filePath;
        this.version = version;
    }

    public Package() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
