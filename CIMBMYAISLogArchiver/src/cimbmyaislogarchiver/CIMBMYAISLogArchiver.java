
package cimbmyaislogarchiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import common.Logging;

/**
 *
 * @author firman.susena
 */
public class CIMBMYAISLogArchiver {
    private static PropertiesLoader pl;
    private static ArrayList<String> logList;
    private static ArrayList<String> extractedLog;
    private static ArrayList<String> extractedTimeoutLog;
    public static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static ch.qos.logback.classic.Logger logger = loggerContext.getLogger("CIMBMYAISLogArchiver");
    
    public CIMBMYAISLogArchiver(){
        logList = new ArrayList<String>();
        logList.add("Custom.access.3.log");
        logList.add("Custom.access.2.log");
        logList.add("Custom.access.1.log");
        logList.add("Custom.access.log");
        
        extractedLog = new ArrayList<String>();
        extractedTimeoutLog = new ArrayList<String>();
    }
    
    private static void extractLog(File logFile, String filterDate){
        try{
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            String record;
            logger.info("Start process file: " + logFile);
            while ((record = br.readLine()) != null) {
                try{
                    String[] data = record.split("\\t");
                    String status = data[9];
                    String trxKey = data[1];
                    if(data[0].contains(filterDate)){
                        extractedLog.add(record);
                        if("#9".equals(status)){
                            if(!"[NA]".equalsIgnoreCase(trxKey)){
                                extractedTimeoutLog.add(trxKey);
                            }
                        }
                    }
                }
                catch(ArrayIndexOutOfBoundsException e)
                {
                    logger.error("Line skipped");
                }
            }
            
            logger.info("Finish process file: " + logFile);
        }catch(FileNotFoundException e){
            System.out.println("File not found: " +logFile );
            logger.info("File not found: " + logFile);

        }catch(IOException e){
            System.out.println(e.getCause());
            logger.error(e.getMessage());

        }
        
    }
    
    public static void main(String[] args) {
        //String log_dir = "C:/ACTIMIZE/ais_server/Instances/RB/logs/access_log/";
        String log_dir = args[0];
        //String filter_date = "2018-Jun-05";
        
        //additional write process log loading properties to get LOG_FILE_DIR
        pl = new PropertiesLoader(args[2]);    
        
        Logging log = new Logging();
        log.configLog(pl,logger,loggerContext);
        logger.info("---------- CIMBMYAISLogArchiver App Started ----------");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd");
        
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String tmpCurrDate = sdf.format(cal.getTime());
        System.out.println(tmpCurrDate);
        logger.info(tmpCurrDate);
        
        String filter_date = tmpCurrDate;
        //String extract_log_dir = "C:/ACTIMIZE/Batch/logs/ais_log_archive/";
        String extract_log_dir = args[1];
        
        CIMBMYAISLogArchiver logArc = new CIMBMYAISLogArchiver();
        
        for(String logName : logList){
            File tmpLog = new File(log_dir+logName);
            System.out.println(tmpLog);
            extractLog(tmpLog, filter_date);
        }
        
        //create log folder date
        File create_extract_log_dir = new File(extract_log_dir+filter_date);
        if (create_extract_log_dir.exists()){
            System.out.println("Log directory exist");
                        logger.info("Log directory exist");

            for(File file: create_extract_log_dir.listFiles()) 
            if (!file.isDirectory()) {
                file.delete();
            }
            create_extract_log_dir.delete();
            System.out.println("Remove log directory");
            logger.info("Remove log directory");
        }
        if (create_extract_log_dir.mkdir()) {
            System.out.println("New Log directory created");
            logger.info("New Log directory created");
        } else {
            System.out.println("Failed to create log directory!");
            logger.info("Failed to create log directory!");
        }
        //end of create log folder date
        
        //generating log
        System.out.println("Generating log");
        logger.info("Generating log");
        try{
            FileWriter rpt_fw = null;
            rpt_fw = new FileWriter(extract_log_dir+filter_date+"/"+filter_date+".txt", true);
            Integer counter = 0;
            for(String record : extractedLog){
                if(counter != extractedLog.size()-1){
                    rpt_fw.write(record+"\n");
                } else{
                    rpt_fw.write(record);
                }
                counter++;
            }
            rpt_fw.flush();
        }catch(IOException e){
            System.out.println(e.getCause());
            logger.error(e.getMessage());
        }
        System.out.println("Log generated with "+extractedLog.size()+ " records");
        logger.info("Log generated with "+extractedLog.size()+ " records");
        //end of generating log
        
        //generating timeout trx key log
        System.out.println("Generating timeout trx key log");
        logger.info("Generating timeout trx key log");
        try{
            FileWriter rpt_fw = null;
            rpt_fw = new FileWriter(extract_log_dir+filter_date+"/timeoutTrxKey_"+filter_date+".txt", true);
            Integer counter = 0;
            for(String record : extractedTimeoutLog){
                if(counter != extractedTimeoutLog.size()-1){
                    rpt_fw.write(record+"\n");
                } else{
                    rpt_fw.write(record);
                }
                counter++;
            }
            rpt_fw.flush();
        }catch(IOException e){
            System.out.println(e.getCause());
            logger.error(e.getMessage());
        }
        System.out.println("Timeout trx key Log generated with "+extractedTimeoutLog.size()+ " records");
        logger.info("Timeout trx key Log generated with "+extractedTimeoutLog.size()+ " records");
        //end of generating timeout trx key log
        
        
        logger.info("---------- CIMBMYAISLogArchiver App Finished ----------");
    }
}
