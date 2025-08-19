package main;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import common.Logging;
import common.PropertiesLoader;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import org.slf4j.LoggerFactory;
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
    private static final SimpleDateFormat archiveDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");    
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

            processLocalFiles(config);
            logger.info("Process completed successfully.");
        } catch (Exception e) {
            logger.error("Fatal error: ", e);
            System.exit(1);
        }
    }
    private static void processSingleFile(File file) {
    try {
        logger.info("Processing file: " + file.getName());
        String fileContent = new String(Files.readAllBytes(file.toPath()));
        
        String[] transactions = fileContent.split(Pattern.quote(config.TRANSACTION_DELIMITER));
        List<Map<String, Object>> allResponses = new ArrayList<>();
        boolean hasInvalidTransaction = false;

        // **CEK DULU semua transaksi sebelum kirim ke API**
        for (String transaction : transactions) {
            if (transaction.trim().isEmpty()) continue;
            
            if (!isValidContent(transaction)) {
                logger.warn("Invalid transaction found: {}", 
                    transaction.substring(0, Math.min(50, transaction.length())));
                hasInvalidTransaction = true;
                break; // **Stop checking, file sudah invalid**
            }
        }

        // **Jika ada yang invalid, langsung pindah ke error folder**
        if (hasInvalidTransaction) {
            logger.warn("File contains invalid transactions - moving to error folder without API calls");
            moveToErrorFolder(file);
            return;
        }

        // **Hanya jika semua valid, baru kirim ke API**
        for (String transaction : transactions) {
            if (transaction.trim().isEmpty()) continue;
            
            Map<String, Object> serviceResponse = sendToService(transaction);
            allResponses.add(serviceResponse);
        }
        
        if (!allResponses.isEmpty()) {
            logResponses(allResponses);
        }
        
        archiveFile(file);
        
    } catch (Exception e) {
        logger.error("Error processing file: " + file.getName(), e);
        moveToErrorFolder(file);
    }
}
    private static boolean isValidContent(String content) {
    if (config.CONTENT_START_PATTERN == null || config.CONTENT_START_PATTERN.isEmpty()) {
        return true;
    }
    
    try {
        Pattern pattern = Pattern.compile(config.CONTENT_START_PATTERN);
        return pattern.matcher(content).find();
        
    } catch (Exception e) {
        logger.error("Content validation error with pattern: {}", config.CONTENT_START_PATTERN, e);
        return false;
    }
}
    private static void moveToErrorFolder(File file) {
    try {
        String errorDir = config.ERROR_DIR;
        Path errorPath = Paths.get(errorDir);
        if (!Files.exists(errorPath)) {
            Files.createDirectories(errorPath);
        }        
        Path destination = errorPath.resolve(file.getName());
        Files.move(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Moved errored file to: {}", destination);
    } catch (Exception e) {
        logger.error("Failed to move errored file {}: {}", file.getName(), e.getMessage(), e);
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
        
        int transactionCount = 1;
        
        for (Map<String, Object> response : responses) {
            ObjectNode formattedResponse = objectMapper.createObjectNode();
            ObjectNode customOut = objectMapper.createObjectNode();
            formattedResponse.set("customOut", customOut);
            
            // Initialize action results with empty values
            ArrayNode actionResults = objectMapper.createArrayNode();
            
            if (response.get("body") != null) {
                try {
                    JsonNode responseBody = objectMapper.readTree(response.get("body").toString());
                    
                    // If the response has the expected structure
                    if (responseBody.has("customOut")) {
                        JsonNode responseCustomOut = responseBody.get("customOut");
                        
                        // Copy all direct fields from customOut
                        responseCustomOut.fields().forEachRemaining(entry -> {
                            String fieldName = entry.getKey();
                            JsonNode value = entry.getValue();
                            
                            if (!"actionResultsSet".equals(fieldName)) {
                                customOut.set(fieldName, value);
                            }
                        });
                        
                        if (responseCustomOut.has("actionResultsSet")) {
                            JsonNode actionResultsSet = responseCustomOut.get("actionResultsSet");
                            if (actionResultsSet.has("actionResultsSet_InnerSet")) {
                                JsonNode innerSet = actionResultsSet.get("actionResultsSet_InnerSet");
                                if (innerSet.isArray()) {
                                    for (JsonNode action : innerSet) {
                                        actionResults.add(action);
                                    }
                                }
                            }
                        }
                    } else {
                        // If response doesn't have customOut structure, try to map the entire body
                        customOut.set("rawResponse", responseBody);
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse response body", e);
                    customOut.put("error", "Failed to parse response");
                }
            } else {
                customOut.put("error", "Empty response body");
            }
            
            ObjectNode actionResultsSet = objectMapper.createObjectNode();
            actionResultsSet.set("actionResultsSet_InnerSet", actionResults);
            customOut.set("actionResultsSet", actionResultsSet);
            
            String prettyResponse = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(formattedResponse);
            
            logger.info("Transaction {} response:\n{}", transactionCount++, prettyResponse);
        }
        
        logger.info("=== End of Transaction Responses ===");
    } catch (Exception e) {
        logger.error("Error logging responses: {}", e.getMessage(), e);
    }
}   
    private static void archiveFile(File file) {
    try {
        String timestamp = archiveDateFormat.format(new Date());
        String archiveDir = config.OUTPUT_DIR + "archive/";
        String zipFileName = archiveDir + file.getName() + "_" + timestamp + ".zip";
        
        Files.createDirectories(Paths.get(archiveDir));
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
             FileInputStream fis = new FileInputStream(file)) {
            
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
        
        Files.delete(file.toPath());
        logger.info("Archived file: {} to {}", file.getName(), zipFileName);
    } catch (Exception e) {
        logger.error("Error archiving file: " + e.getMessage(), e);
    }
}
   }