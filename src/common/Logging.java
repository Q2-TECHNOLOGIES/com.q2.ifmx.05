package common;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 *
 * @author oliviabenazir
 */

public class Logging {
    private static boolean isConfigured = false;

    public void configLog(PropertiesLoader pl, Logger logger, LoggerContext loggerContext) {
        if (isConfigured) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String datenow = dateFormat.format(new Date());

        String logFilePath = pl.path_file_logs + datenow + "_" + pl.file_log_name + ".log";

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss}-%-5p-%-10c:%m%n");
        encoder.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("timestamp");
        fileAppender.setFile(logFilePath);
        fileAppender.setEncoder(encoder);
        fileAppender.setAppend(true);
        fileAppender.start();

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);
        rootLogger.addAppender(fileAppender);

        isConfigured = true;
    }
}

