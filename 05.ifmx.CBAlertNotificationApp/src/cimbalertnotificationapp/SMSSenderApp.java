/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbalertnotificationapp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import javax.net.ssl.HttpsURLConnection;
import model.SMSObject;
//import org.apache.log4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author firman
 */
public class SMSSenderApp {
    //final static Logger logger = Logger.getLogger(SMSSenderApp.class);
    
    public static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static ch.qos.logback.classic.Logger logger = loggerContext.getLogger("SMSSenderApp");
    
    
    public String sendSMS(SMSObject smsObj){
        try{
            HashMap<String, String> responseMap = new HashMap<String, String>();
            responseMap.put("0", "Fail to initialize application");
            responseMap.put("1", "Message received and queued");
            responseMap.put("3", "IP address of request sender is NOT allowed. Access Denied");
            responseMap.put("4", "Missing Input. Either mobile number or message is NOT defined");
            responseMap.put("5", "Invalid mobile number");
            responseMap.put("10", "Invalid request mode, neither text or utf");
            responseMap.put("11", "Invalid User ID");
            responseMap.put("12", "Invalid Channel ID");
            responseMap.put("13", "Invalid timestamp format");
            
            
            System.setProperty("jsse.enableSNIExtension", "false");
            
            String url = smsObj.getSms_url();
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            String param = "tar_num="+smsObj.getTar_num()+"&tar_mode="+smsObj.getTar_mode()+"&tar_msg="+smsObj.getTar_msg()+"&priority="+smsObj.getPriority()+"&user="+smsObj.getSms_user()+"&timestamp="+smsObj.getTimestamp();
            //add reuqest header
            con.setRequestMethod("POST");
            

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(param);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
            }
            in.close();
            System.out.println("######SMS NOTIFICATION#######");
            System.out.println("SMS Response Code : "+response+"");
            System.out.println("SMS Response Message : "+responseMap.get(response+"")+"");
            System.out.println("SMS Number : "+smsObj.getTar_num()+"");
            System.out.println("SMS Message : "+smsObj.getTar_msg()+"");
            System.out.println("######END OF SMS NOTIFICATION#######");
            
            
            logger.info("######SMS NOTIFICATION#######");
            logger.info("SMS Response Code : "+response+"");
            logger.info("SMS Response Message : "+responseMap.get(response+"")+"");
            logger.info("SMS Number : "+smsObj.getTar_num()+"");
            logger.info("SMS Message : "+smsObj.getTar_msg()+"");
            logger.info("######END OF SMS NOTIFICATION#######");
            return response+"";
        } catch(IOException e){
            logger.debug(e.getMessage());
          //logger.debug(e.getCause());
          //logger.debug(e.getStackTrace());
            return "-1";
        }
    }
}
