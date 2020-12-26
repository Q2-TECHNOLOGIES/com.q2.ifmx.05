/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbmyunholdtrxapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
 * @author firman
 */
public class UnholdTrxApp {
    static PropertiesLoader pl = new PropertiesLoader();
    
    public String sendPost(String JSON_STRING) throws Exception {
        System.setProperty("javax.net.ssl.trustStore","C:/Program Files/Java/jdk1.8.0_144/jre/lib/security/cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        
        String url = pl.url;
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
