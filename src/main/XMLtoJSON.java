package main;

import java.io.File;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import common.DatabaseLogger;
import common.Logging;
import common.PropertiesLoader;

public class XMLtoJSON {
    public static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static final Logger logger = loggerContext.getLogger("XMLtoJSON");
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        try {
            
//        String configFilePath = args[0];
//        logger.info("Using config file: {}", configFilePath);
//             File configFile = new File(configFilePath);
//        if (!configFile.exists()) {
//            logger.error("Config file not found at: {}", configFile.getAbsolutePath());
//            System.exit(1);
//        }
            // Initialize properties and logging
            // PropertiesLoader pl = new PropertiesLoader("src/main/resources/config.properties");
//           if (args.length < 2) {
//            // if (args.length < 1) {
//            logger.error("Input validation failed: Requires XML string and config file path");
//            System.exit(1);
//        }
//        
//           String xmlString = args[0];
//           String configFilePath = args[1];
             String configFilePath = args[0];
             String xmlString = """
                            <Response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="FF_bulkPaymentsResponseMessage_V1.xsd">
                            <BulkPayment detectionStatus="success" filename="bulkPaymentXML_170614.xml" transactionKey="111111" batchId="33333" logicalInputFileCreationDateTime="2014-06-17 09:04:00" logicalFileSequenceId="54536565" validEntriesCount="5" invalidEntriesCount="0" numberOfSuccessfullyDetectedEntries="5" numberOfFailedDetectedEntries="0">
                            <BulkPaymentTransactionIdentityFieldsList>
                            <TransactionIdentityField transactionIdentityFieldName="transactionKey" transactionIdentityFieldValue="111111"/>
                            </BulkPaymentTransactionIdentityFieldsList>
                            <BulkPaymentVersionIdentityFieldsList>
                            <VersionIdentityField versionIdentityFieldName="transactionKey" versionIdentityFieldValue="111111"/>
                            <VersionIdentityField versionIdentityFieldName="transactionNormalizedDateTime" versionIdentityFieldValue="2014-06-17 09:04:00"/>
                            </BulkPaymentVersionIdentityFieldsList>
                            <BulkPaymentActions isAlertGenerated="1" response="block">
                            <BulkPaymentAction bulkPaymentActionName="response" bulkPaymentActionValue="block"/>
                            <BulkPaymentAction bulkPaymentActionName="sendSMS" bulkPaymentActionValue="yes"/>
                            </BulkPaymentActions>
                            <BulkPaymentResults actimizeAnalyticsRiskScore="100" userAnalyticsScore=""/>
                            <EntriesResults maxActimizeTransactionRiskScore="100" maxUserAnalyticsScore=""/>
                            <EntriesActions mostSevereRiskLevel="High">
                            <EntryAction entryActionName="riskLevel" mostSevereValue="High"/>
                            <EntryAction entryActionName="sendSMS" mostSevereValue="yes"/>
                            </EntriesActions>
                            </BulkPayment>
                            </Response>""";
//            logger.info("Input parameters validated");
//            logger.debug("XML input length: {} characters", xmlString.length());

            convertXmlToJson(xmlString, configFilePath);        
//            logCompletionStats(startTime);
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

//            long startTime = System.currentTimeMillis();
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
            if (pl.endpoint_URL == null || pl.endpoint_URL.trim().isEmpty()) {
            logger.error("Endpoint URL is not configured in properties file");
            throw new IllegalArgumentException("API endpoint URL is required");
        }
            // postJsonToEndpoint(outputJson.toString(), pl.endpoint_URL);
            postJsonToEndpoint(outputJson.toString(), pl.endpoint_URL, pl);

//            return outputJson;

        } catch (JSONException e) {
            logger.error("XML parsing failed: {}", e.getMessage(), e);
//            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during conversion: {}", e.getMessage(), e);
//            return null;
        }
    }
    public static void postJsonToEndpoint(String jsonPayload,String endpointUrl, PropertiesLoader pl) {
       logger.info("Initializing HTTP POST request to {}", endpointUrl);
    //    logger.debug("Request payload size: {} bytes", jsonPayload.length());
    //    logger.info("Converted JSON payload:\n{}", formatJson(jsonPayload));

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
        logger.info("Response Body: {}", response.body()); // Add this line to log the response body
        logger.debug("Response Headers: {}", response.headers().map());
        DatabaseLogger dbLogger = new DatabaseLogger(pl);
        dbLogger.logStatusCode(String.valueOf(response.statusCode()));
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
    private static void logCompletionStats(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Execution time: {} ms", duration);
        logger.info("Memory used: {} MB", 
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
        logger.info("----------------PROCESS COMPLETED----------------");
    }
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