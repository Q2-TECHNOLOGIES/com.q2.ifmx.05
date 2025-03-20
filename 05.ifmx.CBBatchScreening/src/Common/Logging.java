/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Common;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 *
 * @author Haikal
 */
public class Logging {
    public void configLog(PropertiesLoader pl, Logger logger, LoggerContext loggerContext) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String datenow = dateFormat.format(new Date());
        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext((Context)loggerContext);
        fileAppender.setName("timestamp");
        fileAppender.setFile(pl.log_file_dir + datenow + "_IFMXBatchScreening.log");
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext((Context)loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss}-%-5p-%-10c:%m%n");
        encoder.start();
        fileAppender.setEncoder((Encoder)encoder);
        fileAppender.start();
        logger.addAppender((Appender)fileAppender);
    }
}
