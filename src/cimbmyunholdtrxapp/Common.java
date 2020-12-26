/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cimbmyunholdtrxapp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import javax.net.ssl.SSLSession;
import org.json.simple.JSONObject;

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
}
