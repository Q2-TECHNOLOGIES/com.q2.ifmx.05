/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbmyaislogarchiver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author aliff
 */
public class PropertiesLoader {
private Properties prop = null;
    
    //database properties
    public final String log_file_dir;
    
    
    public PropertiesLoader(String prop_location){
        prop = new Properties();
        
        
        
        InputStream input = null;
        try {
            input = new FileInputStream(prop_location);
            prop.load(input);
    	} catch (IOException io) {
            System.out.println("Unable to load properties file. "+io);
        }
        
        log_file_dir = prop.getProperty("LOG_FILE_DIR");

        
    
    }
}