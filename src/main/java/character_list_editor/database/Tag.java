package character_list_editor.database;

import java.awt.Color;
import java.util.Objects;

public class Tag {
    private int id;
    private String name;
    private Color color;

    public Tag() {}

    public Tag(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public int getColorRGB() {
        return color == null ? 0 : color.getRGB();
    }

    public void setColorRGB(int rgb) {
        this.color = new Color(rgb, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return id == tag.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}