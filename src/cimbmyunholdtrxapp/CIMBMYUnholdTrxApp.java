/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbmyunholdtrxapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import javax.net.ssl.HttpsURLConnection;
import org.json.simple.JSONObject;

/**
 *
 * @author firman.susena
 */
public class CIMBMYUnholdTrxApp {
    private static String generateSignVal(Date dt, String auditNo, String transactionType, String transactionKey) {
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String txtdate = sdf.format(dt);

            String completeText = txtdate+auditNo+transactionType+transactionKey;

            byte[] data = completeText.getBytes();
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            digester.update(data);
            return Base64.getEncoder().encodeToString(digester.digest());
        } catch (NoSuchAlgorithmException e){
            System.out.println(e.getCause());
            return null;
        }
    }

    private static JSONObject generateJSONObj(String auditNo, String transactionType, String transactionKey, String action){
        Date currDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
        String txtCurrDate1 = sdf.format(currDate);
        String txtCurrDate2 = sdf2.format(currDate);
        String txtCurrDate = txtCurrDate1+"T"+txtCurrDate2;
        String signVal = generateSignVal(currDate, auditNo, transactionType, transactionKey);
        if(signVal != null){
            JSONObject obj = new JSONObject();

            JSONObject reqHeader = new JSONObject();
            reqHeader.put("signVal", signVal);
            reqHeader.put("timestamp", txtCurrDate);
            reqHeader.put("auditNo", auditNo);

            JSONObject reqBody = new JSONObject();
            reqBody.put("transactionKey", transactionKey);
            reqBody.put("transactionType", transactionType);
            reqBody.put("action", action);

            obj.put("requestAody", reqHeader);
            obj.put("requestBody", reqBody);
            return obj;
        }
        return null;
    }
    
    // HTTP POST request
    private void sendPost() throws Exception {

        String url = "https://selfsolve.apple.com/wcResults.do";
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "TestingUserAgent");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());
    }
    
    public static void main(String[] args) {
        
    }
}
