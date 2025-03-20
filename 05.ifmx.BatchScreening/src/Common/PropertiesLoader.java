/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import main.Decryptor;


/**
 *
 * @author Haikal
 */
public class PropertiesLoader {
    public Properties prop = null;
    
    public final String log_file_dir;
    
    /*public final String db_host;
    public final String db_port;
    public final String db_username;
    public final String db_name;
    public final String db_password;*/
    
    public final String FTP_Host;
    public final String FTP_Username;
    public final String FTP_Password;
    public final String FTP_Directory;
    public final String Local_Directory;
    public final String delimiter;
    public final String serviceurl;
    public int isDeletedFTPS;
    public int FTP_Port;
    
    
    
    
    
    HashMap<String, String> responseMap = new HashMap();

    public PropertiesLoader(String prop_location) throws Exception {
        Decryptor dec = new Decryptor();
        prop = new Properties();  // Initialize the Properties object

        try {
            FileInputStream propFile = new FileInputStream(prop_location);
            this.prop.load(propFile);  // Load the properties file
            propFile.close();  // Close the file input stream

            // Initialize your properties
            
            this.log_file_dir = this.prop.getProperty("LOG_FILE_DIR");
            
            /*this.db_host = this.prop.getProperty("DB_Host");
            this.db_port = this.prop.getProperty("DB_Port");
            this.db_username = this.prop.getProperty("DB_Username");
            this.db_name = this.prop.getProperty("DB_Name");
            this.db_password = dec.decrypt(this.prop.getProperty("DB_Password"));*/
            
            this.FTP_Host = this.prop.getProperty("FTPS_Host");
            this.FTP_Username = this.prop.getProperty("FTPS_User_Login");
            this.FTP_Password = dec.decrypt(this.prop.getProperty("FTPS_Password"));
            this.FTP_Directory = this.prop.getProperty("Source_File_Directory");
            this.Local_Directory = this.prop.getProperty("Destination_File_Directory");
            this.delimiter = this.prop.getProperty("Delimiter");
            this.serviceurl = this.prop.getProperty("Actimize_URL");
            this.isDeletedFTPS = Integer.parseInt(this.prop.getProperty("Is_Deleted_FTPS_File"));
            this.FTP_Port = Integer.parseInt(this.prop.getProperty("FTPS_Port"));
            
            

        } catch (FileNotFoundException e) {
            throw new Exception("Properties file not found: " + prop_location, e);
        } catch (IOException e) {
            throw new Exception("Error loading properties file: " + prop_location, e);
        } catch (Exception e) {
            throw new Exception("An error occurred while initializing PropertiesLoader", e);
        }  
    }
}
