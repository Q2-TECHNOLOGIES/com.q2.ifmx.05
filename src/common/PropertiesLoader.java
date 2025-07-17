package common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStream;
import java.util.Properties;
//import main.Decryptor;

public class PropertiesLoader {
    private Properties prop = null;
    
    //database properties
    public final String db_host;
    public final String db_port;
    public final String db_udm_name;
    public final String db_udm_stg_schema;
    public final String db_udm_stg_username;
//    public final String db_udm_stg_password;
    
    public final String path_file_backup;
    public final String path_file_error;
    public final String path_file_input;
    public final String endpoint_URL;
    public final String path_file_logs;
    
    public final String file_log_name;
    
    public PropertiesLoader(String prop_location) throws Exception{
        prop = new Properties();
        
//        Decryptor dec = new Decryptor();
        
       InputStream input = null;
       try {
           input = new FileInputStream(prop_location);
           prop.load(input);
   	} catch (IOException io) {
           System.out.println("Unable to load properties file. "+io);
       }
        
        db_host = prop.getProperty("db_host");
        db_port = prop.getProperty("db_port");
        db_udm_name = prop.getProperty("db_udm_name");
        db_udm_stg_schema = prop.getProperty("db_udm_stg_schema");
        db_udm_stg_username = prop.getProperty("db_udm_stg_username");
        
        //db_password = prop.getProperty("db_password");
//        db_udm_stg_password = dec.decrypt(prop.getProperty("db_udm_stg_password"));
        
        path_file_backup  = prop.getProperty("path_file_backup");
        path_file_error = prop.getProperty("path_file_error");
        path_file_input = prop.getProperty("path_file_input");
        endpoint_URL = prop.getProperty("endpoint_URL");
        path_file_logs = prop.getProperty("path_file_logs");
        
        
        file_log_name = prop.getProperty("file_log_name");
    }
}
