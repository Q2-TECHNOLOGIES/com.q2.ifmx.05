package common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStream;
import java.util.Properties;
//import main.Decryptor;

public class PropertiesLoader {
    private Properties prop = null;
    public final String DB_HOST;
    public final String DB_PORT;
    public final String DB_ACTONE_NAME;
    public final String DB_ACTONE_USERNAME;
    public final String DB_ACTONE_PASSWORD;
    public final String ENDPOINT_URL;
    public final String LOG_FILE_DIR;
    public final String CUSTOM_FIELD_NAME;
    public final String TABLE_CUSTOM_FIELD;
    
    public final String FILE_LOG_NAME;

    public PropertiesLoader(String prop_location) throws Exception{
        prop = new Properties();        
       InputStream input = null;
       try {
           input = new FileInputStream(prop_location);
           prop.load(input);
   	} catch (IOException io) {
           System.out.println("Unable to load properties file. "+io);
       }
        
        DB_HOST = prop.getProperty("DB_HOST");
        DB_PORT = prop.getProperty("DB_PORT");
        DB_ACTONE_NAME = prop.getProperty("DB_ACTONE_NAME");
        DB_ACTONE_USERNAME = prop.getProperty("DB_ACTONE_USERNAME");
        DB_ACTONE_PASSWORD = prop.getProperty("DB_ACTONE_PASSWORD");
        CUSTOM_FIELD_NAME = prop.getProperty("CUSTOM_FIELD_NAME");
        TABLE_CUSTOM_FIELD = prop.getProperty("TABLE_CUSTOM_FIELD");
        ENDPOINT_URL = prop.getProperty("ENDPOINT_URL");
        LOG_FILE_DIR = prop.getProperty("LOG_FILE_DIR");
        
        
        FILE_LOG_NAME = prop.getProperty("FILE_LOG_NAME");
    }
}
