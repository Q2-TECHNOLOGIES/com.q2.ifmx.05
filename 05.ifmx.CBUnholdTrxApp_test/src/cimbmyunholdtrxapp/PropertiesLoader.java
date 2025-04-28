package cimbmyunholdtrxapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class PropertiesLoader {
    public final String host;
    //cr ngc
    //public final String url;
    public final String clicks_unhold_url;
    public final String ngc_unhold_url;
    //end of cr ngc
    
    //database properties
    public final String db_host;
    public final String db_port;
    public final String db_name;
    public final String db_username;
    public final String db_password;
    public final String db_connection_driver;
    
    private static final String UNICODE_FORMAT = "UTF8";
    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private KeySpec ks;
    private SecretKeyFactory skf;
    private Cipher cipher;
    byte[] arrayBytes;
    private String myEncryptionKey;
    private String myEncryptionScheme;
    //public final String log_file_dir;
    public final String unhold_log_file_dir;
    SecretKey key;
    
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
    
    public PropertiesLoader(){
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
        
        
        
        String prop_location = "Z:/ACTIMIZE/Batch/app.properties";
        Properties prop = new Properties();
        
        InputStream input = null;
        try {
            input = new FileInputStream(prop_location);
            prop.load(input);
    	} catch (IOException io) {
            System.out.println("Unable to load properties file. "+io);
        }
        
        host = prop.getProperty("host");
        
        //cr ngc
//        url = prop.getProperty("url");
        clicks_unhold_url = prop.getProperty("clicks_unhold_url");
        ngc_unhold_url = prop.getProperty("ngc_unhold_url");
        
        //end of cr ngc
        
        db_host = prop.getProperty("db_host");
        db_port = prop.getProperty("db_port");
        db_name = prop.getProperty("db_name");
        db_username = prop.getProperty("db_username");
        db_password = decrypt(prop.getProperty("db_password"));
        //db_password = "123456";
        db_connection_driver = prop.getProperty("db_connection_driver");
        //log_file_dir = prop.getProperty("LOG_FILE_DIR");
        //unhold_log_file_dir = prop.getProperty("UNHOLD_LOG_FILE_DIR");
        unhold_log_file_dir = prop.getProperty("LOG_FILE_DIR");

    }
}
