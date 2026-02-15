package character_list_editor.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LocaleManager {
    private static final Logger logger = LoggerFactory.getLogger(LocaleManager.class);
    private static LocaleManager instance;

    private String currentLocale;
    private final Map<String, ResourceBundle> bundles = new HashMap<>();
    private static final String LOCALES_DIR = "locales";

    private LocaleManager() {
        loadAvailableLocales();
        setLocale("ru");
    }

    public static synchronized LocaleManager inst() {
        if (instance == null) {
            instance = new LocaleManager();
        }
        return instance;
    }

    private void loadAvailableLocales() {
        File localesDir = PathUtil.getAppFile(LOCALES_DIR);
        if (!localesDir.exists() || !localesDir.isDirectory()) {
            logger.warn("Locales directory not found: {}", localesDir.getAbsolutePath());
            return;
        }

        logger.info("Loading locales from directory: {}", localesDir.getAbsolutePath());

        File[] localeFiles = localesDir.listFiles((dir, name) ->
                name.endsWith(".locale") && new File(dir, name).isFile()
        );

        if (localeFiles == null) {
            logger.error("Error accessing locales directory");
            return;
        }

        if (localeFiles.length == 0) {
            logger.warn("No locale files found in directory: {}", localesDir.getAbsolutePath());
            return;
        }

        for (File file : localeFiles) {
            String fileName = file.getName();
            String localeCode = fileName.substring(0, fileName.lastIndexOf('.'));

            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

                ResourceBundle bundle = new PropertyResourceBundle(isr);
                bundles.put(localeCode, bundle);
                logger.info("Successfully loaded locale: {} ({} keys)", localeCode, bundle.keySet().size());

            } catch (IOException e) {
                logger.error("Error loading locale file: {} - {}", fileName, e.getMessage());
            }
        }

        logger.info("Total loaded locales: {}", bundles.size());
    }

    public String getString(String key) {
        if (currentLocale == null || !bundles.containsKey(currentLocale)) {
            logger.debug("Key '{}' not found for locale '{}'", key, currentLocale);
            return key;
        }
        try {
            return bundles.get(currentLocale).getString(key);
        } catch (MissingResourceException e) {
            logger.debug("Key '{}' not found in locale '{}'", key, currentLocale);
            return key;
        }
    }

    public void setLocale(String localeCode) {
        if (bundles.containsKey(localeCode)) {
            currentLocale = localeCode;
            logger.info("Locale changed to: {}", localeCode);
        } else {
            logger.warn("Locale not available: {}", localeCode);
        }
    }

    public List<String> getAvailableLocales() {
        return new ArrayList<>(bundles.keySet());
    }

    public String getCurrentLocale() {
        return currentLocale;
    }
}