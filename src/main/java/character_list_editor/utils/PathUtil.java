package character_list_editor.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class PathUtil {

    public static String APP_DIR;

    static {
        URL location = PathUtil.class.getProtectionDomain()
                .getCodeSource()
                .getLocation();

        try {
            APP_DIR = new File(location.toURI()).getParentFile().getAbsolutePath();
        }
        catch (URISyntaxException e) {
            String rawPath = location.getPath();
            String decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);

            if (isWindows() && decodedPath.matches("^/[a-zA-Z]:/.*")) {
                decodedPath = decodedPath.substring(1);
            }
            File jarFile = new File(decodedPath);
            APP_DIR = jarFile.getParentFile().getAbsolutePath();
        }

    }

    public static File getAppFile(String path) {
        File file;

        if (new File(path).isAbsolute()) {
            file = new File(path);
        } else {
            file = Paths.get(APP_DIR, path).toFile();
        }
        return file;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}

