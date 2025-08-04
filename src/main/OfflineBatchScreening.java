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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import org.slf4j.LoggerFactory;
import com.jcraft.jsch.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class OfflineBatchScreening {
    private static PropertiesLoader config;
    public static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static final org.slf4j.Logger logger = loggerContext.getLogger("OfflineBatch");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        // File localFile = new File(localPath);
        
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
    private static void processSingleFile(File file) {
        try {
            logger.info("Processing file: " + file.getName());
            String fileContent = new String(Files.readAllBytes(file.toPath()));
            
            String[] transactions = fileContent.split(Pattern.quote(config.TRANSACTION_DELIMITER));
        List<Map<String, Object>> allResponses = new ArrayList<>(); 

            for (String transaction : transactions) {
                if (transaction.trim().isEmpty()) continue;
                
                String transformedContent = transformToActimizeFormat(transaction, "");
                Map<String, Object> serviceResponse = sendToService(transformedContent);
                allResponses.add(serviceResponse);
            }
            if (!allResponses.isEmpty()) {
                logResponses(allResponses); 
            }
        
            
            // Archive the file
            archiveFile(file);
            
        } catch (Exception e) {
            logger.error("Error processing file: " + file.getName(), e);
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
    private static String cleanJsonString(String json) {
    return json.replace('\u00A0', ' ')  
              .replace('\u200B', ' ')  
              .replace('\uFEFF', ' '); 
}
    private static String transformToActimizeFormat(String originalMessage, String apiResponse) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode customOut = objectMapper.createObjectNode();
        root.set("customOut", customOut);

        try {
            String cleanedMessage = cleanJsonString(originalMessage);
            JsonNode inputJson = objectMapper.readTree(cleanedMessage);
            JsonNode genericNode = inputJson.path("generic");
            
            // Transaction details
            customOut.put("transactionKey", genericNode.path("baseTransactionC").path("transactionKey").asText(""));
            customOut.put("transactionType", genericNode.path("baseTransactionC").path("transactionType").asText(""));
            
            // Required Actimize fields
            customOut.put("actimizeAnalyticsScore", 0); // Must be numeric
            customOut.put("actimizeTransactionRiskScore", 0); // Must be numeric
            customOut.put("userAnalyticsScore", 0); // Must be numeric
            customOut.put("isAlertGenerated", ""); // Must be boolean
            customOut.put("responseCode", "");

            // Action results
            ArrayNode actionResults = objectMapper.createArrayNode();
            addEmptyActionResult(actionResults, "Action on Online User ID");
            addEmptyActionResult(actionResults, "Alert");
            addEmptyActionResult(actionResults, "Email To Internal");
            addEmptyActionResult(actionResults, "Response");
            
            ObjectNode actionResultsSet = objectMapper.createObjectNode();
            actionResultsSet.set("actionResultsSet_InnerSet", actionResults);
            customOut.set("actionResultsSet", actionResultsSet);

            // Numeric fields with proper defaults
            addNumericFieldSafely(customOut, genericNode, "amount", "originalAmount", 0);
            addNumericFieldSafely(customOut, genericNode, "amount", "normalizedOriginalAmount", 0);
            addNumericFieldSafely(customOut, genericNode, "transferTransaction", "paymentSpeedCd", 0);
            
            // Add party and account details
            customOut.put("partyKey", genericNode.path("baseTransactionA").path("partyKey").asText(""));
            customOut.put("accountKey", genericNode.path("baseTransactionB").path("accountKey").asText(""));
            customOut.put("userId", genericNode.path("baseTransactionA").path("userId").asText(""));
            
            // Add timestamp fields
            customOut.put("transactionLocalDateTime", genericNode.path("baseTransactionA").path("transactionLocalDateTime").asText(""));
            customOut.put("transactionNormalizedDateTime", genericNode.path("baseTransactionA").path("transactionNormalizedDateTime").asText(""));
            
        } catch (Exception e) {
            logger.error("Error transforming message, returning empty structure", e);
        }
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }
    private static void addNumericFieldSafely(ObjectNode target, JsonNode source, String parentField, String fieldName, double defaultValue) {
        if (source.has(parentField)) {
            JsonNode parentNode = source.get(parentField);
            if (parentNode.has(fieldName) && !parentNode.get(fieldName).isNull()) {
                JsonNode valueNode = parentNode.get(fieldName);
                if (valueNode.isNumber()) {
                    target.put(fieldName, valueNode.asDouble());
                } else if (valueNode.isTextual()) {
                    try {
                        target.put(fieldName, Double.parseDouble(valueNode.asText()));
                    } catch (NumberFormatException e) {
                        target.put(fieldName, defaultValue);
                    }
                }
            } else {
                target.put(fieldName, defaultValue);
            }
        } else {
            target.put(fieldName, defaultValue);
        }
    }
    private static void addEmptyActionResult(ArrayNode array, String name) {
    array.add(objectMapper.createObjectNode()
        .put("Name", name)
        .put("Value", ""));  
}
    private static Map<String, Object> sendToService(String message) {
    Map<String, Object> result = new HashMap<>();
    HttpURLConnection  conn = null;
    try {
        URL url = new URL(config.ACTIMIZE_URL);
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        String cleanedJson = cleanJsonString(message);
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = cleanedJson.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Get status code
        int statusCode = conn.getResponseCode();
        result.put("status_code", statusCode);
        
        // Read response
        StringBuilder response = new StringBuilder();
        try (InputStream is = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine);
            }
            result.put("body", response.toString());
        }
        
    } catch (IOException e) {
        int errorCode = 500;
        String errorResponse = "{\"error\":\"" + e.getMessage() + "\"}";
        
        if (conn != null) {
            try {
                errorCode = conn.getResponseCode();
                try (InputStream es = conn.getErrorStream();
                     BufferedReader br = new BufferedReader(new InputStreamReader(es, "utf-8"))) {
                    StringBuilder errorBody = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorBody.append(errorLine);
                    }
                    errorResponse = errorBody.toString();
                }
            } catch (IOException ioException) {
                errorResponse = "{\"error\":\"Failed to get error response\"}";
            }
        }
        
        result.put("status_code", errorCode);
        result.put("body", errorResponse);
    } finally {
        if (conn != null) {
            conn.disconnect();
        }
    }
    return result;
}
    private static void logResponses(List<Map<String, Object>> responses) {
    try {
        logger.info("=== Transaction Responses ===");
        
        ObjectMapper mapper = new ObjectMapper();
        int transactionCount = 1;
        
        for (Map<String, Object> response : responses) {
            ObjectNode enhancedResponse = mapper.createObjectNode();
            enhancedResponse.put("http_status", (Integer) response.get("status_code"));
            
            try {
                JsonNode responseBody = mapper.readTree(response.get("body").toString());
                enhancedResponse.set("api_response", responseBody);
            } catch (Exception e) {
                enhancedResponse.put("api_response", response.get("body").toString());
            }
            
            String formattedResponse = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(enhancedResponse);
            
            logger.info("Transaction {} response:\n{}", transactionCount++, formattedResponse);
        }
        
        logger.info("=== End of Transaction Responses ===");
    } catch (Exception e) {
        logger.error("Error logging responses: {}", e.getMessage(), e);
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
       
    return encrypted; 
    }
}