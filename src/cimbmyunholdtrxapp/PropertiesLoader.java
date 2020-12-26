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
    public final String url;
    
    //database properties
    public final String db_host;
    public final String db_port;
    public final String db_name;
    public final String db_username;
    public final String db_password;
    
    private static final String UNICODE_FORMAT = "UTF8";
    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private KeySpec ks;
    private SecretKeyFactory skf;
    private Cipher cipher;
    byte[] arrayBytes;
    private String myEncryptionKey;
    private String myEncryptionScheme;
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
        
        
        
        String prop_location = "D:/ACTIMIZE/Batch/app.properties";
        Properties prop = new Properties();
        
        InputStream input = null;
        try {
            input = new FileInputStream(prop_location);
            prop.load(input);
    	} catch (IOException io) {
            System.out.println("Unable to load properties file. "+io);
        }
        
        host = prop.getProperty("host");
        url = prop.getProperty("url");
        
        db_host = prop.getProperty("db_host");
        db_port = prop.getProperty("db_port");
        db_name = prop.getProperty("db_name");
        db_username = prop.getProperty("db_username");
        //db_password = prop.getProperty("db_password");
        db_password = decrypt(prop.getProperty("db_password"));
    }
}
