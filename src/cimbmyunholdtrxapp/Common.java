/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbmyunholdtrxapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import javax.net.ssl.SSLSession;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author firman.susena
 */
public class Common {
    private static PropertiesLoader pl;
    
    public Common(PropertiesLoader pl){
        this.pl = pl;
    }
    
    static{
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
            new javax.net.ssl.HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession sess) {
                    if(hostname.equals(pl.host)){
                        return true;
                    } else{
                        return false;
                    }
                }
            }
        );
    }
    
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

    public static JSONObject generateJSONObj(String auditNo, String transactionType, String transactionKey, String action){
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
            //reqHeader.put("serviceName", "unholdDomesticTransfer");
            reqHeader.put("signVal", signVal);
            reqHeader.put("timestamp", txtCurrDate);
            reqHeader.put("auditNo", auditNo);

            JSONObject reqBody = new JSONObject();
            reqBody.put("transactionKey", transactionKey);
            reqBody.put("transactionType", transactionType);
            reqBody.put("action", action);

//            JSONObject reqHeader = new JSONObject();
//            reqHeader.put("serviceName", "unholdDomesticTransfer");
//            reqHeader.put("signVal", "p09h0MQoyosj+lzA/yZGapFk1hI5JX+EOJ3JND7qHco=");
//            reqHeader.put("timestamp", "2015-12-05T23:11:05");
//            reqHeader.put("auditNo", "123456789012345678");
//
//            JSONObject reqBody = new JSONObject();
//            reqBody.put("transactionKey", "2016032511223380054125");
//            reqBody.put("transactionType", "BP0101");
//            reqBody.put("action", "HR");

            obj.put("requestHeader", reqHeader);
            obj.put("requestBody", reqBody);
            return obj;
        }
        return null;
    }
    
    public String sendPost(String JSON_STRING, String url) throws Exception {
        System.setProperty("javax.net.ssl.trustStore","C:/Program Files/Java/jdk1.8.0_144/jre/lib/security/cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        
        //cr ngc
        //String url = pl.url;
        //end of cr ngc
        HttpClient client = HttpClientBuilder.create().build();
	HttpPost post = new HttpPost(url);

	StringEntity requestEntity = new StringEntity(JSON_STRING, ContentType.APPLICATION_JSON);
	post.setEntity(requestEntity);

	HttpResponse response = client.execute(post);

	int responseCode = response.getStatusLine().getStatusCode();
        String respCode = responseCode+"";

	//System.out.println("\nSending 'POST' request to URL : " + url);
	//System.out.println("Post parameters : " + JSON_STRING);
        
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

	StringBuffer respMessage = new StringBuffer();
	String line = "";
	while ((line = rd.readLine()) != null) {
		respMessage.append(line);
	}
        
        //response json parser
        JSONParser parser = new JSONParser();
        
        try{
            JSONObject json = (JSONObject) parser.parse(respMessage.toString());
            String body = json.get("responseBody").toString();
            JSONObject json_body = (JSONObject) parser.parse(body);
            //System.out.println(json_body.get("statusCode"));
            respCode = json_body.get("statusCode").toString();
        } catch (ParseException e){
            System.out.println(e.getCause());
            respCode = responseCode+"";
        } catch (NullPointerException e){
            System.out.println(e.getCause());
            respCode = responseCode+"";
        } 
        
        //System.out.println("Response Message : "+respMessage);
        
        return respCode+";"+respMessage.toString();
  }
}
