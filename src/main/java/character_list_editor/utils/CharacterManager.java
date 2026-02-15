package character_list_editor.utils;

import character_list_editor.window.CharacterFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }
}
