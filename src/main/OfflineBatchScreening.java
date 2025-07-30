package main;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import common.Logging;
import common.PropertiesLoader;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.util.regex.Pattern;
import java.net.URL;
import java.text.SimpleDateFormat;
import org.slf4j.LoggerFactory;
import com.jcraft.jsch.*;

public class OfflineBatchScreening {
    private static PropertiesLoader config;
    public static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static final org.slf4j.Logger logger = loggerContext.getLogger("OfflineBatch");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static int retryCount = 0;
    
    public static void main(String[] args) {    
        try {
            String configPath = args[0];
            runBatchScreening(configPath); 
        } catch (Exception e) {
            logger.error("Initialization failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    public static void runBatchScreening(String configFilePath) {
        logger.info("Starting Offline Batch Screening Process");
        Logging logging = new Logging();
        try {
            config = new PropertiesLoader(configFilePath);
            logging.configLog(config, (ch.qos.logback.classic.Logger) logger, loggerContext);

            if (Integer.parseInt(config.IS_DOWNLOAD) == 1) {
                downloadFilesFromSFTP(config);
            }
            
            processLocalFiles(config);
            logger.info("Process completed successfully.");
        } catch (Exception e) {
            logger.error("Fatal error: ", e);
            System.exit(1);
        }
    }

    private static void downloadFilesFromSFTP(PropertiesLoader pl) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        boolean error = false;
        
        try {
            logger.info("Connecting to SFTP server...");
            session = jsch.getSession(pl.SFTP_USER, pl.SFTP_HOST, Integer.parseInt(pl.SFTP_PORT));
            
            // Login with decrypted password
            String decryptedPwd = decryptPassword(pl.SFTP_PASSWORD);
            session.setPassword(decryptedPwd);
            
            // Avoid host key verification (not recommended for production)
            Properties sftpConfig = new Properties();
            sftpConfig.put("StrictHostKeyChecking", "no");
            session.setConfig(sftpConfig);
            
            session.connect();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            
            logger.info("Connected to SFTP server successfully");
            
            // Change to working directory
            channelSftp.cd(pl.SFTP_DIRECTORY);
            Vector<ChannelSftp.LsEntry> files = channelSftp.ls("*");
            
            if (files != null && files.size() > 0) {
                logger.info("Found " + files.size() + " files to download");
                
                for (ChannelSftp.LsEntry entry : files) {
                    if (!entry.getAttrs().isDir()) {
                        downloadSingleFile(channelSftp, entry.getFilename(), pl);
                    }
                }
            } else {
                logger.info("No files found on SFTP server");
            }
            
        } catch (Exception e) {
            error = true;
            logger.error("SFTP error: " + e.getMessage());
            
            // Retry logic
            if (retryCount < Integer.parseInt(pl.MAX_RETRY)) {
                retryCount++;
                logger.info("Retrying connection (attempt " + retryCount + ")...");
                
                try {
                    TimeUnit.SECONDS.sleep(Integer.parseInt(pl.RETRY_DELAY_SECONDS));
                    downloadFilesFromSFTP(pl);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                logger.error("Max retry attempts reached. Exiting...");
                System.exit(1);
            }
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
            
            if (error && retryCount >= Integer.parseInt(pl.MAX_RETRY)) {
                System.exit(1);
            }
        }
    }
    
    private static void downloadSingleFile(ChannelSftp channelSftp, String filename, PropertiesLoader pl) throws Exception {
        String localPath = pl.INPUT_DIR + filename;
        File localFile = new File(localPath);
        
        try {
            channelSftp.get(filename, localPath);
            logger.info("Downloaded file: " + filename);
            
            // Delete from SFTP if configured
            if (Integer.parseInt(pl.IS_DELETE_AFTER_DOWNLOAD) == 1) {
                channelSftp.rm(filename);
                logger.info("Deleted remote file: " + filename);
            }
        } catch (Exception e) {
            logger.error("Failed to download file: " + filename, e);
            throw e;
        }
    }
    
    private static void processLocalFiles(PropertiesLoader pl) {
        File dir = new File(pl.INPUT_DIR);
        File[] files = dir.listFiles();
        
        if (files != null && files.length > 0) {
            logger.info("Processing " + files.length + " local files");
            
            for (File file : files) {
                if (file.isFile()) {
                    processSingleFile(file);
                }
            }
        } else {
            logger.info("No files found in local directory");
        }
    }
    
    private static void processSingleFile(File file) {
        try {
            logger.info("Processing file: " + file.getName());
            String content = new String(Files.readAllBytes(file.toPath()));
            String[] messages = content.split(Pattern.quote(config.TRANSACTION_DELIMITER));            
            for (String message : messages) {
                String response = sendToService(message.trim());
                logResponse(response);
            }
            
            archiveFile(file);
        } catch (Exception e) {
            logger.error("Error processing file " + file.getName() + ": " + e.getMessage(), e);
        }
    }
    
    private static String sendToService(String message) {
        try {
            // Create SSL context that trusts all certificates (for demo only)
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) {}
                public void checkServerTrusted(X509Certificate[] xcs, String string) {}
                public X509Certificate[] getAcceptedIssuers() { return null; }
            }}, new SecureRandom());
            
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            
            URL url = new URL(config.ACTIMIZE_URL);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = message.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            
            return response.toString();
        } catch (Exception e) {
            logger.error("Error sending to service: " + e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    private static void logResponse(String response) {
        try {
            String logDir = config.LOG_FILE_DIR;
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String logFile = logDir + "response_" + timestamp + ".log";
            
            Files.write(Paths.get(logFile), response.getBytes(), 
                      StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            logger.info("Response logged to: " + logFile);
        } catch (Exception e) {
            logger.error("Error logging response: " + e.getMessage(), e);
        }
    }
    
    private static void archiveFile(File file) {
        try {
            String archiveDir = config.OUTPUT_DIR + "archive/";
            Files.createDirectories(Paths.get(archiveDir));
            
            Path source = file.toPath();
            Path target = Paths.get(archiveDir + file.getName());
            
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Archived file: " + file.getName());
        } catch (Exception e) {
            logger.error("Error archiving file: " + e.getMessage(), e);
        }
    }
    
    private static String decryptPassword(String encrypted) {
        // Implement your decryption logic here
        return encrypted; // Placeholder - replace with actual decryption
    }
}