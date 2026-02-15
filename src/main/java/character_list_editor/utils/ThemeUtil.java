package character_list_editor.utils;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class ThemeUtil {

    private static final FlatLaf darkTheme = new FlatDarkLaf();
    private static final FlatLaf lightTheme = new FlatLightLaf();

    private static final Logger logger = LoggerFactory.getLogger(ThemeUtil.class);

    public static void updateTheme() {
        try {
            switch (ConfigManager.getInstance().getTheme()) {
                case "dark":
                    UIManager.setLookAndFeel(darkTheme);
                    break;
                case "light":
                default:
                    UIManager.setLookAndFeel(lightTheme);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.repaint();
        }
    }

}
