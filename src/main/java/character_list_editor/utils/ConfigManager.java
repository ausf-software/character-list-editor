package character_list_editor.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private static final String CONFIG_FILE = "config.json";
    private static final String DEFAULT_THEME = "light";
    private static final String DEFAULT_LOCALE = "ru";
    private static final String DEFAULT_CHARACTER_NAME = "No name";
    private static final String DEFAULT_CHARACTERS_DIR = new File(PathUtil.APP_DIR, "Characters").getAbsolutePath();

    private static ConfigManager instance;
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode config;
    private final File configFile;

    private ConfigManager() {
        this.configFile = PathUtil.getAppFile(CONFIG_FILE);
        loadConfig();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadConfig() {
        try {
            if (configFile.exists()) {
                config = mapper.readTree(configFile);
                logger.info("Configuration loaded from: {}", configFile.getAbsolutePath());
            } else {
                config = mapper.createObjectNode();
                setTheme(DEFAULT_THEME);
                setLocale(DEFAULT_LOCALE);
                setDefaultCharacterName(DEFAULT_CHARACTER_NAME);
                setDefaultCharacterDirectory(DEFAULT_CHARACTERS_DIR);
                saveConfig();
                logger.info("New configuration file created with default values");
            }

            if (!config.has("theme")) {
                logger.debug("Theme not found in config, setting default: {}", DEFAULT_THEME);
                setTheme(DEFAULT_THEME);
            }
            if (!config.has("locale")) {
                logger.debug("Locale not found in config, setting default: {}", DEFAULT_LOCALE);
                setLocale(DEFAULT_LOCALE);
            }
            if (!config.has("defaultCharacterDirectory")) {
                logger.debug("Default character directory not found, setting default: {}", DEFAULT_CHARACTERS_DIR);
                setDefaultCharacterDirectory(DEFAULT_CHARACTERS_DIR);
            }
            if (!config.has("character_name")) {
                logger.debug("Default character name not found, setting default: {}", DEFAULT_CHARACTER_NAME);
                setDefaultCharacterName(DEFAULT_CHARACTER_NAME);
            }

        } catch (Exception e) {
            logger.error("Error loading configuration: {}", e.getMessage());
            config = mapper.createObjectNode();
            setTheme(DEFAULT_THEME);
            setLocale(DEFAULT_LOCALE);
            setDefaultCharacterDirectory(DEFAULT_CHARACTERS_DIR);
            setDefaultCharacterName(DEFAULT_CHARACTER_NAME);
            saveConfig();
            logger.info("Configuration reset to defaults due to error");
        }
    }

    public String getTheme() {
        return config.path("theme").asText(DEFAULT_THEME);
    }

    public void setTheme(String theme) {
        if (config instanceof ObjectNode) {
            ((ObjectNode) config).put("theme", theme);
            saveConfig();
            logger.info("Theme changed to: {}", theme);
        } else {
            logger.warn("Failed to set theme: config is not an ObjectNode");
        }
    }

    public String getLocale() {
        return config.path("locale").asText(DEFAULT_LOCALE);
    }

    public void setLocale(String locale) {
        if (config instanceof ObjectNode) {
            ((ObjectNode) config).put("locale", locale);
            saveConfig();
            logger.info("Locale changed to: {}", locale);
        } else {
            logger.warn("Failed to set locale: config is not an ObjectNode");
        }
    }

    public String getDefaultCharacterDirectory() {
        return config.path("defaultCharacterDirectory").asText(DEFAULT_CHARACTERS_DIR);
    }

    public void setDefaultCharacterDirectory(String path) {
        if (config instanceof ObjectNode) {
            ((ObjectNode) config).put("defaultCharacterDirectory", path);
            saveConfig();
            logger.info("Default character directory changed to: {}", path);
        } else {
            logger.warn("Failed to set default character directory: config is not an ObjectNode");
        }
    }

    private void saveConfig() {
        try {
            configFile.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
            logger.debug("Configuration saved successfully to: {}", configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error saving configuration: {}", e.getMessage());
        }
    }

    public String getDefaultCharacterName() {
        return config.path("character_name").asText(DEFAULT_CHARACTER_NAME);
    }

    public void setDefaultCharacterName(String name) {
        if (config instanceof ObjectNode) {
            ((ObjectNode) config).put("character_name", name);
            saveConfig();
            logger.info("Default character name changed to: {}", name);
        } else {
            logger.warn("Failed to set Default character name: config is not an ObjectNode");
        }
    }
}
