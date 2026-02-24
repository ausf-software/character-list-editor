package character_list_editor.utils;

import character_list_editor.database.Character; // возможно, нужен этот импорт
import character_list_editor.window.CharacterFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CharacterManager {

    private static final Logger logger = LoggerFactory.getLogger(CharacterManager.class);
    private static CharacterManager instance;

    public static CharacterManager getInstance() {
        if (instance == null) {
            instance = new CharacterManager();
        }
        return instance;
    }

    public void openCharacter(String path, CharacterFrame characterFrame) throws CharacterLoadException {
        // реализация...
    }

    /**
     * Возвращает список всех персонажей.
     * @return список персонажей (не null)
     */
    public List<Character> getAllCharacters() {
        // Здесь должна быть логика загрузки всех персонажей
        // Например, из базы данных или файловой системы
        return new ArrayList<>();
    }
}