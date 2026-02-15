package character_list_editor.utils;

public class CharacterLoadException extends Exception {
    public CharacterLoadException(String message) {
        super(message);
    }

    public CharacterLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}