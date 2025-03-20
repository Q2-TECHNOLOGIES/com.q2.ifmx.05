/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbalertnotificationapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Properties;

import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author 4dm1n
 */
public class PropertiesLoader {
    private Properties prop = null;
    
    //database properties
    public final String db_host;
    public final String db_port;
    public final String db_name;
    public final String db_username;
    public final String db_password;
    
    public final String db_idb_name;
    public final String db_idb_username;
    public final String db_idb_password;
    public final String log_file_dir;
    
    //email properties
    public final String email_username;
    public final String email_password;
    public final String smtp_host;
    public final String smtp_port;
    public final String email_group_recipient;
    
    //sms properties
    public final String sms_url;
    public final String sms_user;
    
    private static final String UNICODE_FORMAT = "UTF8";
    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private KeySpec ks;
    private SecretKeyFactory skf;
    private Cipher cipher;
    byte[] arrayBytes;
    private String myEncryptionKey;
    private String myEncryptionScheme;
    SecretKey key;
    
    HashMap<String, String> responseMap = new HashMap<String, String>();
    
    public final String decrypt(String encryptedString) {
        String decryptedText=null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedText = Base64.decodeBase64(encryptedString);
            byte[] plainText = cipher.doFinal(encryptedText);
            decryptedText= new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedText;
    }
    
    public PropertiesLoader(String prop_location){
        try{
            myEncryptionKey = "CIMBCIMBMalaysiaCIMBMYMY";
            myEncryptionScheme = DESEDE_ENCRYPTION_SCHEME;
            arrayBytes = myEncryptionKey.getBytes(UNICODE_FORMAT);
            ks = new DESedeKeySpec(arrayBytes);
            skf = SecretKeyFactory.getInstance(myEncryptionScheme);
            cipher = Cipher.getInstance(myEncryptionScheme);
            key = skf.generateSecret(ks);
        } catch(InvalidKeyException e){
            System.out.println(e.getCause());
        } catch(NoSuchAlgorithmException e){
            System.out.println(e.getCause());
        } catch(UnsupportedEncodingException e){
            System.out.println(e.getCause());
        } catch(NoSuchPaddingException e){
            System.out.println(e.getCause());
        } catch(InvalidKeySpecException e){
            System.out.println(e.getCause());
        }
        
        prop = new Properties();
        
        InputStream input = null;
        try {
            input = new FileInputStream(prop_location);
            prop.load(input);
    	} catch (IOException io) {
            System.out.println("Unable to load properties file. "+io);
        }
        
        db_host = prop.getProperty("db_host");
        db_port = prop.getProperty("db_port");
        db_name = prop.getProperty("db_name");
        db_username = prop.getProperty("db_username");
        db_password = decrypt(prop.getProperty("db_password"));
        //db_password = prop.getProperty("db_password");
        
        db_idb_name = prop.getProperty("db_idb_name");
        db_idb_username = prop.getProperty("db_idb_username");
        db_idb_password = decrypt(prop.getProperty("db_idb_password"));
        
        sms_url = prop.getProperty("SMS_URL");
        sms_user = prop.getProperty("SMS_USER");
        
        email_username = prop.getProperty("EMAIL_USER");
        email_password = prop.getProperty("EMIAL_PASSWORD");
        smtp_host = prop.getProperty("SMTP_HOST");
        smtp_port = prop.getProperty("SMTP_PORT");
        email_group_recipient = prop.getProperty("EMAIL_GROUP_RECIPIENT");
        log_file_dir = prop.getProperty("LOG_FILE_DIR");

        
        responseMap.put("Decline","Hold");
        responseMap.put("Challenge","Hold");
        responseMap.put("Delay","Hold");
        responseMap.put("Allow","No Hold");
    }
}
