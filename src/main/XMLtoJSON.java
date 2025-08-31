package main;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import common.Logging;
import common.PropertiesLoader;

public class XMLtoJSON {
    public static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static final Logger logger = loggerContext.getLogger("XMLtoJSON");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
     try {
        // Handle Error Case (3 arguments)
        if (args.length == 3 && args[0].startsWith("ERROR")) {
            generateErrorETLToJson(args[0], args[1], args[2]); // ERROR_MESSAGE, FILENAME, CONFIG_PATH
        } 
        // Handle Actimize Error Case (2 arguments with ERROR_ACTIMIZE)
        else if (args.length == 2 && args[0].startsWith("ERROR_ACTIMIZE")) {
            convertErrorActimizeToJson(args[0], args[1]); // ERROR_MESSAGE, CONFIG_PATH
        }
        else if (args.length == 2) {
            convertXmlToJson(args[0], args[1]);
        }
        else {
            logger.error("Invalid arguments. Expected:");
            logger.error("1. Error Case (3 args): ERROR_MESSAGE FILENAME CONFIG_PATH");
            logger.error("2. Actimize Error Case (2 args): ERROR_ACTIMIZE_MESSAGE CONFIG_PATH");
            logger.error("3. Normal Case (2 args): XML_STRING CONFIG_PATH");
            System.exit(1);
        }
    } catch (Exception e) {
        logger.error("Initialization failed: {}", e.getMessage(), e);
        System.exit(1);
    }
}
    public static void convertXmlToJson(String xmlString, String configFilePath) {
        logger.info("Starting XML to JSON conversion");
        Logging logging = new Logging();
        try {
        PropertiesLoader pl = new PropertiesLoader(configFilePath);
        logging.configLog(pl, (ch.qos.logback.classic.Logger) logger, loggerContext);
            logger.info("----------------XML TO JSON CONVERSION STARTED----------------");
            logger.info("Process started at {}", dateFormat.format(new Date()));

            // PropertiesLoader pl = new PropertiesLoader(configFilePath);
            JSONObject jsonObj = XML.toJSONObject(xmlString);
            JSONObject bulkPayment = jsonObj.getJSONObject("Response").getJSONObject("BulkPayment");

        // Create ordered JSON object using LinkedHashMap
        JSONObject outputJson = new JSONObject(new LinkedHashMap<>());
        // Map all direct attributes with safe value handling
        outputJson.put("batchId", safeGetString(bulkPayment, "batchId"));
        outputJson.put("transactionKey", safeGetString(bulkPayment, "transactionKey"));
        outputJson.put("detectionStatus", safeGetString(bulkPayment, "detectionStatus"));
        String originalFilename = safeGetString(bulkPayment, "filename");
        String csvFilename = originalFilename.replaceAll("(?i)\\.xml$", ".csv");
        outputJson.put("filename", csvFilename);
        outputJson.put("logicalInputFileCreationDateTime", safeGetString(bulkPayment, "logicalInputFileCreationDateTime"));
        outputJson.put("logicalFileSequenceId", safeGetString(bulkPayment, "logicalFileSequenceId"));
        outputJson.put("validEntriesCount", safeGetString(bulkPayment, "validEntriesCount"));
        outputJson.put("invalidEntriesCount", safeGetString(bulkPayment, "invalidEntriesCount"));
        outputJson.put("numberOfSuccessfullyDetectedEntries", safeGetString(bulkPayment, "numberOfSuccessfullyDetectedEntries"));
        outputJson.put("numberOfFailedDetectedEntries", safeGetString(bulkPayment, "numberOfFailedDetectedEntries"));
        
        // Process transactionNormalizedDateTime from VersionIdentityField
        JSONObject versionFields = bulkPayment.getJSONObject("BulkPaymentVersionIdentityFieldsList");
        JSONArray versionFieldsArray = versionFields.getJSONArray("VersionIdentityField");
        for (int i = 0; i < versionFieldsArray.length(); i++) {
            JSONObject field = versionFieldsArray.getJSONObject(i);
            if (safeGetString(field, "versionIdentityFieldName").equals("transactionNormalizedDateTime")) {
                outputJson.put("transactionNormalizedDateTime", safeGetString(field, "versionIdentityFieldValue"));
                break;
            }
        }
            
        // Process BulkPaymentActions
        JSONObject bulkPaymentActions = bulkPayment.getJSONObject("BulkPaymentActions");
        JSONObject actionsOutput = new JSONObject();
        actionsOutput.put("isAlertGenerated", safeGetString(bulkPaymentActions, "isAlertGenerated"));
        actionsOutput.put("response", safeGetString(bulkPaymentActions, "response"));
        
        JSONArray actionArray = new JSONArray();
        if (bulkPaymentActions.get("BulkPaymentAction") instanceof JSONArray) {
            JSONArray actions = bulkPaymentActions.getJSONArray("BulkPaymentAction");
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                actionArray.put(new JSONObject()
                    .put("bulkPaymentActionName", safeGetString(action, "bulkPaymentActionName"))
                    .put("bulkPaymentActionValue", safeGetString(action, "bulkPaymentActionValue")));
            }
        } else {
            JSONObject action = bulkPaymentActions.getJSONObject("BulkPaymentAction");
            actionArray.put(new JSONObject()
                .put("bulkPaymentActionName", safeGetString(action, "bulkPaymentActionName"))
                .put("bulkPaymentActionValue", safeGetString(action, "bulkPaymentActionValue")));
        }
        actionsOutput.put("BulkPaymentAction", actionArray);
        outputJson.put("BulkPaymentActions", actionsOutput);
        
        // Process BulkPaymentResults
        JSONObject bulkPaymentResults = bulkPayment.getJSONObject("BulkPaymentResults");
        JSONObject resultsOutput = new JSONObject();
        resultsOutput.put("actimizeAnalyticsRiskScore", safeGetString(bulkPaymentResults, "actimizeAnalyticsRiskScore"));
        resultsOutput.put("userAnalyticsScore", safeGetString(bulkPaymentResults, "userAnalyticsScore"));
        outputJson.put("BulkPaymentResults", resultsOutput);
        
        // Process EntriesResults
        JSONObject entriesResults = bulkPayment.getJSONObject("EntriesResults");
        JSONObject entriesResultsOutput = new JSONObject();
        entriesResultsOutput.put("maxActimizeTransactionRiskScore", safeGetString(entriesResults, "maxActimizeTransactionRiskScore"));
        entriesResultsOutput.put("maxUserAnalyticsScore", safeGetString(entriesResults, "maxUserAnalyticsScore"));
        outputJson.put("EntriesResults", entriesResultsOutput);
        
        // Process EntriesActions
        JSONObject entriesActions = bulkPayment.getJSONObject("EntriesActions");
        JSONObject entriesActionsOutput = new JSONObject();
        entriesActionsOutput.put("mostSevereRiskLevel", safeGetString(entriesActions, "mostSevereRiskLevel"));
        
        if (entriesActions.has("EntryAction")) {
            JSONArray entryActionsArray = new JSONArray();
            if (entriesActions.get("EntryAction") instanceof JSONArray) {
                JSONArray actions = entriesActions.getJSONArray("EntryAction");
                for (int i = 0; i < actions.length(); i++) {
                    JSONObject action = actions.getJSONObject(i);
                    entryActionsArray.put(new JSONObject()
                        .put("entryActionName", safeGetString(action, "entryActionName"))
                        .put("mostSevereValue", safeGetString(action, "mostSevereValue")));
                }
            } else {
                JSONObject action = entriesActions.getJSONObject("EntryAction");
                entryActionsArray.put(new JSONObject()
                    .put("entryActionName", safeGetString(action, "entryActionName"))
                    .put("mostSevereValue", safeGetString(action, "mostSevereValue")));
            }
            entriesActionsOutput.put("EntryAction", entryActionsArray);
        }
        outputJson.put("EntriesActions", entriesActionsOutput);
            
        Random rand = new Random();
        int randomAuditNo = 100000 + rand.nextInt(900000); 
        outputJson.put("auditNo", String.valueOf(randomAuditNo));   

            logger.info("Conversion successful");
            logger.debug("Final JSON output: {}", outputJson.toString(4));
            if (pl.ENDPOINT_URL == null || pl.ENDPOINT_URL.trim().isEmpty()) {
            logger.error("Endpoint URL is not configured in properties file");
            throw new IllegalArgumentException("API endpoint URL is required");
        }
            postJsonToEndpoint(outputJson.toString(), pl.ENDPOINT_URL);
        } catch (JSONException e) {
            logger.error("XML parsing failed: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during conversion: {}", e.getMessage(), e);
        }
    }
    public static void convertErrorActimizeToJson(String errorMessage, String configFilePath) {
        Pattern pattern = Pattern.compile("actimizeInputFileName=\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(errorMessage);

        String fileName = "";
        if (matcher.find()) {
            fileName = matcher.group(1); 
            if (fileName.contains("xml")) {
                fileName = fileName.replace("xml", "csv");
            }
        }

        try {
            PropertiesLoader pl = new PropertiesLoader(configFilePath);
            JSONObject errorJson = new JSONObject()
                .put("BulkPaymentActions", new JSONObject()
                    .put("BulkPaymentAction", new JSONArray()
                        .put(new JSONObject()
                            .put("bulkPaymentActionName", "")
                            .put("bulkPaymentActionValue", "")))
                    .put("isAlertGenerated", "")
                    .put("response", ""))
                .put("BulkPaymentResults", new JSONObject()
                    .put("actimizeAnalyticsRiskScore", "")
                    .put("userAnalyticsScore", ""))
                .put("EntriesResults", new JSONObject()
                    .put("maxActimizeTransactionRiskScore", "")
                    .put("maxUserAnalyticsScore", ""))
                .put("EntriesActions", new JSONObject()
                    .put("mostSevereRiskLevel", ""))
                .put("batchId", "")
                .put("transactionKey", "")
                .put("detectionStatus", "")
                .put("filename", fileName)
                .put("logicalInputFileCreationDateTime", "")
                .put("logicalFileSequenceId", "")
                .put("invalidEntriesCount", "")
                .put("validEntriesCount", "")
                .put("numberOfFailedDetectedEntries", "")
                .put("numberOfSuccessfullyDetectedEntries", "")
                .put("transactionNormalizedDateTime", "")
                .put("auditNo", new Random().nextInt(900000) + 100000)
                .put("errorMessage", errorMessage);

            String endpointUrl = pl.ENDPOINT_URL;
            postJsonToEndpoint(errorJson.toString(), endpointUrl);
        } catch (Exception e) {
            logger.error("Failed to generate error JSON from ", e);
            throw new RuntimeException(e);
        }
    }
    public static void generateErrorETLToJson(String errorMessage, String fileName, String configFilePath) {
    try {
        PropertiesLoader pl = new PropertiesLoader(configFilePath);
        JSONObject errorJson = new JSONObject()
            .put("BulkPaymentActions", new JSONObject()
                .put("BulkPaymentAction", new JSONArray()
                    .put(new JSONObject()
                        .put("bulkPaymentActionName", "")
                        .put("bulkPaymentActionValue", "")))
                .put("isAlertGenerated", "")
                .put("response", ""))
            .put("BulkPaymentResults", new JSONObject()
                .put("actimizeAnalyticsRiskScore", "")
                .put("userAnalyticsScore", ""))
            .put("EntriesResults", new JSONObject()
                .put("maxActimizeTransactionRiskScore", "")
                .put("maxUserAnalyticsScore", ""))
            .put("EntriesActions", new JSONObject()
                .put("mostSevereRiskLevel", ""))
            .put("batchId", "")
            .put("transactionKey", "")
            .put("detectionStatus", "")
            .put("filename", fileName)
            .put("logicalInputFileCreationDateTime", "")
            .put("logicalFileSequenceId", "")
            .put("invalidEntriesCount", "")
            .put("validEntriesCount", "")
            .put("numberOfFailedDetectedEntries", "")
            .put("numberOfSuccessfullyDetectedEntries", "")
            .put("transactionNormalizedDateTime", "")
            .put("auditNo", new Random().nextInt(900000) + 100000)
            .put("errorMessage", errorMessage);

        String endpointUrl = pl.ENDPOINT_URL;
        postJsonToEndpoint(errorJson.toString(), endpointUrl);
    } catch (Exception e) {
        logger.error("Failed to generate error JSON", e);
        throw new RuntimeException(e);
    }
}
    public static void postJsonToEndpoint(String jsonPayload,String endpointUrl) {
       logger.info("Initializing HTTP POST request to {}", endpointUrl);
          try {
            HttpClient client = HttpClient.newBuilder()
                // .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            logger.info("Sending HTTP request...");
                 HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        logger.info("HTTP Response received - Status Code: {}", response.statusCode());
        logger.info("Response Body: {}", response.body()); 
        logger.debug("Response Headers: {}", response.headers().map());
    } catch (IOException e) {
            logger.error("Network error occurred: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("Process interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
    public static void formatJson(String jsonString)
   {
    try {
        JSONObject jsonObject = new JSONObject(jsonString);
        System.out.println(jsonObject.toString(4));
    } catch (JSONException e) {
        System.out.println(jsonString);
    }
}
    // private static void logCompletionStats(long startTime) {
    //     long duration = System.currentTimeMillis() - startTime;
    //     logger.info("Execution time: {} ms", duration);
    //     logger.info("Memory used: {} MB", 
    //         (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
    //     logger.info("----------------PROCESS COMPLETED----------------");
    // }
    private static String safeGetString(JSONObject obj, String key) {
    try {
        if (obj.has(key)) {
            Object value = obj.get(key);
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof Number) {
                return value.toString();
            } else if (value instanceof Boolean) {
                return value.toString();
            }
            return value != null ? value.toString() : "";
        }
        return "";
    } catch (JSONException e) {
        logger.warn("Failed to get value for key {}: {}", key, e.getMessage());
        return "";
    }
}
}