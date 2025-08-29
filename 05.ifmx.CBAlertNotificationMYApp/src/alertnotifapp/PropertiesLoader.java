package alertnotifapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import main.Decryptor;  


public class PropertiesLoader {
    private Properties prop = null;
    
    // PostgreSQL database properties
    public final String db_host;
    public final String db_port;
    public final String db_name;
    public final String db_username;
    public final String db_password;
        
    // IDB database properties
    public final String db_idb_name;
    // Email properties
    public final String email_username;
    public final String email_password;
    public final String smtp_host;
    public final String smtp_port;
    public final String email_group_recipient;

    public final String alert_ready_step_id;
    public final String alert_assigned_status_id;
    public final String bu_assigned_alert;
    public final int max_retry_count;

    public final String list_identifier_email; 
    public final String LOG_FILE_DIR;
    public final String FILE_LOG_NAME;
    

    
    // Response mapping
    public final HashMap<String, String> responseMap = new HashMap<>();
    
    public PropertiesLoader(String prop_location) {
        prop = new Properties();
        
        try (InputStream input = new FileInputStream(prop_location)) {
            prop.load(input);
        } catch (IOException io) {
            System.err.println("Unable to load properties file: " + io.getMessage());
            throw new RuntimeException("Failed to load properties file", io);
        }
        Decryptor decryptor;
        try {
            decryptor = new Decryptor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Decryptor", e);
        }
        // Database Configuration
        db_host = prop.getProperty("db_host");
        db_port = prop.getProperty("db_port");  
        db_name = prop.getProperty("db_name");
        db_username = prop.getProperty("db_username");
        db_password = decryptor.decrypt(prop.getProperty("db_password")); // Decrypt here
        db_idb_name = prop.getProperty("db_idb_name", db_name);  
        
            // Email Configuration
        email_username = prop.getProperty("email.username");
        email_password = decryptor.decrypt(prop.getProperty("email.password")); // Decrypt here
        smtp_host = prop.getProperty("smtp.host");
        smtp_port = prop.getProperty("smtp.port");
        email_group_recipient = prop.getProperty("EMAIL_GROUP_RECIPIENT");
        
        // Initialize response mapping
        // responseMap.put("Decline", "Hold");
        // responseMap.put("Challenge", "Hold");
        // responseMap.put("Delay", "Hold");
        // responseMap.put("Allow", "No Hold");

         // Alert Configuration
        alert_ready_step_id = prop.getProperty("ALERT_READY_STEP_ID");
        alert_assigned_status_id = prop.getProperty("ALERT_ASSIGNED_STATUS_ID");
        bu_assigned_alert = prop.getProperty("BU_ASSIGNED_ALERT");
        list_identifier_email = prop.getProperty("LIST_IDENTIFIER_EMAIL");
            
        // Application Settings
        max_retry_count = Integer.parseInt(prop.getProperty("max.retry.count", "3"));

        LOG_FILE_DIR = prop.getProperty("LOG_FILE_DIR", "");
        FILE_LOG_NAME = prop.getProperty("FILE_LOG_NAME", "application");
    }
}
