/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package xmltojson;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;
import org.json.XML;
/**
 *
 * @author fzulf
 */
public class XMLtoJSON {

 
    /**
     * Explanation
     *   args[0] = isi XML (String)
     *   args[1] = path file JSON untuk testing (misal: /tmp/output.json)
     *   args[2] = URL endpoint tujuan (misal: https://example.com/endpoint)
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java XmlToJsonAndPost <xmlString> <jsonOutputPath> <endpointUrl>");
            System.exit(1);
        }

//        String xmlString      = args[0];        // XML dari AIS-Actimize
String xmlString = """
<Response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:noNamespaceSchemaLocation="FF_bulkPaymentsResponseMessage_V1.xsd">
  <BulkPayment detectionStatus="success" filename="bulkPaymentXML_170614.xml"
               transactionKey="111111" batchId="33333"
               logicalInputFileCreationDateTime="2014-06-17 09:04:00"
               logicalFileSequenceId="54536565" validEntriesCount="5"
               invalidEntriesCount="0" numberOfSuccessfullyDetectedEntries="5"
               numberOfFailedDetectedEntries="0">
    <BulkPaymentTransactionIdentityFieldsList>
      <TransactionIdentityField transactionIdentityFieldName="transactionKey"
                                transactionIdentityFieldValue="111111"/>
    </BulkPaymentTransactionIdentityFieldsList>
    <BulkPaymentVersionIdentityFieldsList>
      <VersionIdentityField versionIdentityFieldName="transactionKey"
                            versionIdentityFieldValue="111111"/>
      <VersionIdentityField versionIdentityFieldName="transactionNormalizedDateTime"
                            transactionIdentityFieldValue="2014-06-17 09:04:00"/>
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
</Response>
""";
        String jsonOutputPath = args[0];        // Temp file JSON (untuk testing)
        String endpointUrl    = args[1];

//        String jsonOutputPath = args[1];        // Temp file JSON (untuk testing)
//        String endpointUrl    = "https://httpbin.org/post";


        try {
            // 1. Konversi XML ke JSONObject
            JSONObject jsonObj = XML.toJSONObject(xmlString);

            // 2. Simpan JSONObject ke file (format pretty-printed)
            saveJsonToFile(jsonObj, jsonOutputPath);

            // 3. Hit endpoint dengan JSON (atau bisa kirim XML asli jika diperlukan)
            //    Di contoh ini kita kirim JSON; kalau nanti endpoint butuh XML, tinggal ganti body-nya.
            postJsonToEndpoint(jsonObj.toString(), endpointUrl);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Simpan JSONObject ke file dalam bentuk JSON yang terformat (pretty-printed).
     */
    private static void saveJsonToFile(JSONObject jsonObj, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            // Indent factor = 4 agar mudah dibaca
            writer.write(jsonObj.toString(4));
            System.out.println("Berhasil menyimpan JSON ke: " + outputPath);
        } catch (IOException ex) {
            System.err.println("Gagal menulis file JSON: " + ex.getMessage());
        }
    }

    /**
     * Kirim HTTP POST dengan Content-Type: application/json
     */
    private static void postJsonToEndpoint(String jsonPayload, String endpointUrl) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            // Blocking call, tunggu response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("=== HTTP POST ke " + endpointUrl + " selesai ===");
            System.out.println("Status Code : " + response.statusCode());
            System.out.println("Response Body : " + response.body());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error saat POST ke endpoint: " + e.getMessage());
        }
    }
}
