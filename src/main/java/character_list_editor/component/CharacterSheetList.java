package character_list_editor.component;

import character_list_editor.database.Character;

import javax.swing.*;
import java.util.function.Predicate;

public class CharacterSheetList extends JPanel {
    public void setFilter(Predicate<Character> filter) {
    }

    public Character getSelectedCharacter() {
        return null;
    }
}
