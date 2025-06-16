package xmltojson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;

public class XMLtoJSON {
    private static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final Logger logger = loggerContext.getLogger("XMLtoJSON");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        logger.info("----------------XML TO JSON CONVERSION STARTED----------------");
        logger.info("Process started at {}", dateFormat.format(new Date()));

        // Input Validation
//        if (args.length < 1) {
//            logger.error("Input validation failed: No XML string provided");
//            logger.info("Usage: java XMLtoJSON <xmlString>");
//            logger.info("Process aborted with exit code 1");
//            System.exit(1);
//        }

//        String xmlString = args[0];
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
        String endpointUrl = "https://httpbin.org/post";
        
        logger.info("Input parameters validated");
        logger.debug("XML input length: {} characters", xmlString.length());
        logger.info("Target endpoint: {}", endpointUrl);

        try {
            // Conversion Process
            logger.info("Starting XML to JSON conversion");
            JSONObject jsonObj = XML.toJSONObject(xmlString);
            logger.info("Conversion successful");
            logger.debug("JSON output length: {} characters", jsonObj.toString().length());
            
            // HTTP Post
            postJsonToEndpoint(jsonObj.toString(), endpointUrl);

        } catch (JSONException e) {
            logger.error("XML parsing failed: {}", e.getMessage(), e);
            logger.error("Failed XML snippet: {}", xmlString.substring(0, Math.min(xmlString.length(), 100)));
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("----------------PROCESS COMPLETED----------------");
            logger.info("Total execution time: {} ms", duration);
            logger.info("Memory used: {} MB", 
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
        }
    }

    private static void postJsonToEndpoint(String jsonPayload, String endpointUrl) {
        logger.info("Initializing HTTP POST request to {}", endpointUrl);
        logger.debug("Request payload size: {} bytes", jsonPayload.length());
        logger.info("Converted JSON payload:\n{}", formatJson(jsonPayload));

        try {
            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            logger.info("Sending HTTP request...");
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("HTTP Response received");
            logger.info("Status Code: {}", response.statusCode());
            logger.debug("Response Headers: {}", response.headers().map());
            logger.info("Response Body Length: {} bytes", response.body().length());

        } catch (IOException e) {
            logger.error("Network error occurred: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("Process interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
    private static String formatJson(String jsonString) {
    try {
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject.toString(4); 
    } catch (JSONException e) {
        return jsonString; 
}
}