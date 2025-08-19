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
    public final String ERROR_DIR;

    // Processing
    public final String TRANSACTION_DELIMITER;

    // Endpoint (your config uses ACTIMIZE_URL; code reads SERVICE_URL)
    public final String ACTIMIZE_URL;

    // App behavior / retries
    public final String MAX_RETRY;
    public final String RETRY_DELAY_SECONDS;

    public final String CONTENT_START_PATTERN;

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
        ERROR_DIR = prop.getProperty("ERROR_DIR");


        // Processing
        TRANSACTION_DELIMITER = prop.getProperty("TRANSACTION_DELIMITER");

        // Endpoint (bridge naming)
        ACTIMIZE_URL = prop.getProperty("ACTIMIZE_URL");
        // App behavior / retries
        MAX_RETRY = prop.getProperty("MAX_RETRY");
        RETRY_DELAY_SECONDS = prop.getProperty("RETRY_DELAY_SECONDS");
        CONTENT_START_PATTERN = prop.getProperty("CONTENT_START_PATTERN");
    }

}
