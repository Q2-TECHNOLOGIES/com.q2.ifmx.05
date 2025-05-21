/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.text.SimpleDateFormat;
import java.util.Date;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;
import cimbmyaislogarchiver.PropertiesLoader;
//import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author aliff
 */
public class Logging {
public void configLog (PropertiesLoader pl, Logger logger, LoggerContext loggerContext){
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String datenow = dateFormat.format(new Date());    

        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("timestamp");
        // set the file name
        fileAppender.setFile(pl.log_file_dir+datenow+"_CIMBMYAISLogArchiver.log");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss}-%-5p-%-10c:%m%n");
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // attach the rolling file appender to the logger of your choice
        logger.addAppender(fileAppender);

        // OPTIONAL: print logback internal status messages
        //StatusPrinter.print(loggerContext);
        
    }
    
}
