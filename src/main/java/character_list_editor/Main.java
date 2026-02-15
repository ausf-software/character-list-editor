package character_list_editor;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import character_list_editor.utils.ConfigManager;
import character_list_editor.utils.LocaleManager;
import character_list_editor.utils.PathUtil;
import character_list_editor.utils.ThemeUtil;
import character_list_editor.window.StartupFrame;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class Main {
    public static final String VERSION = "1.0";

    public static void main(String[] args) {
        configureLogPath(PathUtil.APP_DIR);
        ConfigManager config = ConfigManager.getInstance();
        LocaleManager localeManager = LocaleManager.inst();
        ThemeUtil.updateTheme();
        localeManager.setLocale(config.getLocale());

        SwingUtilities.invokeLater(() -> {
            StartupFrame startupFrame = new StartupFrame();
            startupFrame.setVisible(true);
        });
    }

    public static void configureLogPath(String logPath) {
        logPath = logPath + "/logs/";
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = context.getLogger("ROOT");

        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setFile(logPath + "/character-list-editor.log");

        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logPath + "/character-list-editor.%d{yyyy-MM-dd}.log");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.start();

        fileAppender.setRollingPolicy(rollingPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        rootLogger.addAppender(fileAppender);
    }
}
