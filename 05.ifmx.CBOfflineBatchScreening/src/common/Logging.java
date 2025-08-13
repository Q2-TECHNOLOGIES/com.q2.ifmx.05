package common;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 *
 * @author oliviabenazir
 */

public class Logging {
    private static boolean isConfigured = false;
    private static String lastLogDate = "";

    public void configLog(PropertiesLoader pl, Logger logger, LoggerContext loggerContext) {
        // if (isConfigured) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String datenow = dateFormat.format(new Date());

        boolean needsReconfigure = !datenow.equals(lastLogDate);

        // Check if the existing appender's file still exists
        FileAppender<ILoggingEvent> existingAppender = (FileAppender<ILoggingEvent>) loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
            .getAppender("timestamp");
        if (existingAppender != null) {
            File logFile = new File(existingAppender.getFile());
            if (!logFile.exists() || !logFile.canWrite()) {
                needsReconfigure = true;
                loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender("timestamp");
            }
        } else {
            needsReconfigure = true;
        }

        if (!needsReconfigure && isConfigured) {
            return;
        }

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        if (existingAppender != null) {
            existingAppender.stop();
            rootLogger.detachAppender("timestamp");
        }
        String logFilePath = pl.LOG_FILE_DIR + datenow + "_" + pl.FILE_LOG_NAME + ".log";

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

        rootLogger.setLevel(Level.DEBUG);
        rootLogger.addAppender(fileAppender);

        isConfigured = true;
        lastLogDate = datenow;
        logger.info("Logging configured to: {}", logFilePath);
    }

    }
