package common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {
    private Properties prop = null;

    // Logging and directories
    public final String LOG_FILE_DIR;
    public final String FILE_LOG_NAME;
    public final String INPUT_DIR;
    public final String OUTPUT_DIR;
    public final String LOCAL_DIRECTORY;   // alias for INPUT_DIR

    // Processing
    public final String TRANSACTION_DELIMITER;

    // Endpoint (your config uses ACTIMIZE_URL; code reads SERVICE_URL)
    public final String ACTIMIZE_URL;
    public final String SERVICE_URL;       // alias for ACTIMIZE_URL

    // “SFTP” settings (keys as in config)
    public final String SFTP_HOST;
    public final String SFTP_PORT;
    public final String SFTP_USER;
    public final String SFTP_PASSWORD;
    public final String SFTP_DIRECTORY;

    // App behavior / retries
    public final String IS_DOWNLOAD;
    public final String MAX_RETRY;
    public final String RETRY_DELAY_SECONDS;
    public final String IS_DELETE_AFTER_DOWNLOAD;

    public PropertiesLoader(String prop_location) throws Exception {
        prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(prop_location);
            prop.load(input);
        } catch (IOException io) {
            System.out.println("Unable to load properties file. " + io);
        } finally {
            if (input != null) try { input.close(); } catch (IOException ignored) {}
        }

        // Logging and directories
        LOG_FILE_DIR = prop.getProperty("LOG_FILE_DIR");
        FILE_LOG_NAME = prop.getProperty("FILE_LOG_NAME");

        INPUT_DIR = prop.getProperty("INPUT_DIR");
        OUTPUT_DIR = prop.getProperty("OUTPUT_DIR");
        LOCAL_DIRECTORY = (prop.getProperty("LOCAL_DIRECTORY") != null)
                ? prop.getProperty("LOCAL_DIRECTORY")
                : INPUT_DIR; // keep current code working

        // Processing
        TRANSACTION_DELIMITER = prop.getProperty("TRANSACTION_DELIMITER");

        // Endpoint (bridge naming)
        ACTIMIZE_URL = prop.getProperty("ACTIMIZE_URL");
        SERVICE_URL = (prop.getProperty("SERVICE_URL") != null)
                ? prop.getProperty("SERVICE_URL")
                : ACTIMIZE_URL;

        // SFTP settings
        SFTP_HOST = prop.getProperty("SFTP_HOST");
        SFTP_PORT = prop.getProperty("SFTP_PORT");
        SFTP_USER = prop.getProperty("SFTP_USER");
        SFTP_PASSWORD = prop.getProperty("SFTP_PASSWORD");
        SFTP_DIRECTORY = prop.getProperty("SFTP_DIRECTORY");

        // App behavior / retries
        IS_DOWNLOAD = prop.getProperty("IS_DOWNLOAD");
        MAX_RETRY = prop.getProperty("MAX_RETRY");
        RETRY_DELAY_SECONDS = prop.getProperty("RETRY_DELAY_SECONDS");
        IS_DELETE_AFTER_DOWNLOAD = prop.getProperty("IS_DELETE_AFTER_DOWNLOAD");
    }

    // optional convenience if you still need to access the raw Properties somewhere
    public Properties asProperties() {
        Properties p = new Properties();
        p.putAll(prop);
        // keep aliases in sync
        if (SERVICE_URL != null) p.setProperty("SERVICE_URL", SERVICE_URL);
        if (LOCAL_DIRECTORY != null) p.setProperty("LOCAL_DIRECTORY", LOCAL_DIRECTORY);
        return p;
    }
}
