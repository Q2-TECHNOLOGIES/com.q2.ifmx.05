package cimbmyunholdtrxapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

public class UnholdTrx {
    final static Logger logger = Logger.getLogger(UnholdTrx.class);
    
    public UnholdTrx(){
        
    }
    private static Connection getDBConnection(String db_host, String db_port, String db_name, String db_username, String db_password) {
        Connection conn = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            String db_url = "jdbc:jtds:sqlserver://"+db_host+":"+db_port+"/";
            conn = DriverManager.getConnection(db_url+db_name+"/",db_username,db_password);
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Class not found, error: " + cnfe);
            logger.error("Class not found, error: " + cnfe);
            System.exit(0);
        } catch (SQLException e){
            System.out.println(e.getCause());
            logger.error(e.getCause());
        }
        return conn;
    }
    
    public static String wsCaller(String actionId, String cwi, String twis){
        String result = "";
        PropertiesLoader pl = new PropertiesLoader();
        UnholdTrxApp unholdApp = new UnholdTrxApp();
        Common cmn = new Common(pl);
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmmss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("dd-MM-yyyy");
        String timestamp = dateFormat.format(new Date());
        String datenow = dateFormat2.format(new Date());
        
        Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password);
        
        String[] tmp_twis = twis.split(";");
        String summaryResponseMessage = "";
        for(String twi : tmp_twis){
            Random rand = new Random();
            int n = rand.nextInt(100000)+1;
            if(conn != null){
                try{
                    String query = "SELECT a.p11 as transaction_type, b.csm18 as transaction_key, a.alert_custom_attributes_id FROM alerts a "+
                            "join acm_alert_custom_attributes b on a.alert_custom_attributes_id = b.alert_custom_attributes_id "+
                            "WHERE a.alert_id = '"+twi+"'";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        String auditNo = n+sdf.format(new Date())+"";
                        String trxKey = rs.getString("transaction_key");
                        String trxType = rs.getString("transaction_type");
                        String alert_custom_attributes_id = rs.getString("alert_custom_attributes_id");

                        String action = "HA";
                        if("Block".equalsIgnoreCase(actionId)){
                            action = "HR";
                        }

                        JSONObject obj = cmn.generateJSONObj(auditNo, trxType, trxKey, action);

                        String responseCode = "";
                        String responseMessage = "";
                        
                        //setup log
                        File file = new File("D:/ACTIMIZE/Batch/logs/UNHOLDTRX.log");
                        String lastmodified = dateFormat2.format(file.lastModified());
                        if(!lastmodified.equals(datenow)){
                            SimpleDateFormat tmpsdf = new SimpleDateFormat("ddMMyyHH");
                            String tmplastmodified = tmpsdf.format(file.lastModified());
                            file.renameTo(new File("D:/ACTIMIZE/Batch/logs/UNHOLDTRX_"+tmplastmodified+".log"));
                        }
                        //end of setup log
                        
                        try{
                            result = unholdApp.sendPost(obj.toString());
                            responseMessage = result.substring(result.indexOf(";")+1);
                            responseCode = result.substring(0, result.indexOf(";"));
                            if("000".equals(responseCode)){
                                try(FileWriter fw = new FileWriter("D:/ACTIMIZE/Batch/logs/UNHOLDTRX.log", true);
                                    BufferedWriter bw = new BufferedWriter(fw);
                                    PrintWriter out = new PrintWriter(bw)){
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:==============");
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:PROCESSING TWI : "+twis);
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:REQUEST MESSAGE : "+obj.toString());
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE CODE : "+responseCode);
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE MESSAGE : "+ "Unhold Transaction(s) successful");
                                    
                                    System.out.println("REQUEST MESSAGE : "+obj.toString());
                                    System.out.println("RESPONSE CODE : "+responseCode);
                                    System.out.println("RESPONSE MESSAGE : "+ responseMessage);
                                    
                                    responseMessage = "Unhold Transaction(s) successful";
                                    System.out.println(responseMessage);

                                    Integer update_result = 0;
                                    try{
                                        String query_update = "update acm_alert_custom_attributes set ci12 = 0 where alert_custom_attributes_id = "+alert_custom_attributes_id;
                                        Statement stmt_update = conn.createStatement();
                                        update_result = stmt_update.executeUpdate(query_update);

                                        if(update_result == 1){
                                            out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : successful");
                                            System.out.println("UPDATE ci12 : successful");
                                            String query_update_unhold_status = "update alerts set p40 = '"+actionId+"' where alert_id = '"+twi+"'";
                                            Statement stmt_update_unhold_status = conn.createStatement();
                                            update_result = stmt_update_unhold_status.executeUpdate(query_update_unhold_status);
                                            if(update_result == 1){
                                                out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 : successful");
                                                System.out.println("UPDATE p40 : successful");
                                            } else{
                                                out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 : failed due to record not found");
                                                System.out.println("UPDATE p40 : failed");
                                            }
                                        } else{
                                            out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : failed due to record not found");
                                            System.out.println("UPDATE ci12 : failed");
                                        }
                                    } catch(SQLException e){
                                        out.println(datenow+"-ERROR - "+e.getMessage());
                                    }
                                    
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);
                                    System.out.println("TIMESTAMP : "+timestamp);
                                } catch (IOException e) {
                                    System.out.println(e.getCause());
                                }
                                summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" successful\n";
                            } else{
                                try(FileWriter fw = new FileWriter("D:/ACTIMIZE/Batch/logs/UNHOLDTRX.log", true);
                                    BufferedWriter bw = new BufferedWriter(fw);
                                    PrintWriter out = new PrintWriter(bw)){

                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:==============");
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:REQUEST MESSAGE : "+obj.toString());
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE CODE : "+responseCode);
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE MESSAGE : "+ responseMessage);
                                    out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);

                                    System.out.println("REQUEST MESSAGE : "+obj.toString());
                                    System.out.println("RESPONSE CODE : "+responseCode);
                                    System.out.println("RESPONSE MESSAGE : "+ responseMessage);
                                    System.out.println("TIMESTAMP : "+timestamp);
                                } catch (IOException e) {
                                    System.out.println(e.getCause());
                                }
                                summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" failed. Error code : 500\n";
                            }
                        } catch (Exception e){
                            try(FileWriter fw = new FileWriter("D:/ACTIMIZE/Batch/logs/UNHOLDTRX.log", true);
                                    BufferedWriter bw = new BufferedWriter(fw);
                                    PrintWriter out = new PrintWriter(bw)){
                                out.println(e.getCause());
                                out.println(e.getMessage());
                                out.println(e.getStackTrace());
                            } catch(IOException exception){
                                System.out.println(exception.getCause());
                            }
                            
                            //System.out.println(e.getMessage());
                            System.out.println("REQUEST MESSAGE : "+obj.toString());
                            System.out.println("RESPONSE CODE : "+responseCode);
                            System.out.println("RESPONSE MESSAGE : "+ responseMessage);
                            System.out.println("TIMESTAMP : "+timestamp);
                            //return "Unhold transaction(s) failed with Error Code : "+responseCode+". Please contact your system administrator.";
                            summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" failed. Error code "+responseCode+"\n";
                        }
                    }
                } catch(SQLException e){
                    System.out.println(e.getCause());
                    logger.error(e.getCause());
                    return "SQL Exception : "+e.getCause();
                }
            }
        }
        return summaryResponseMessage;
    }
    
    public static void main(String[] args) {
//        UnholdTrx u = new UnholdTrx();
//        System.out.println(u.wsCaller("Accept","twi","RMO_0000015121;"));
    }
}
