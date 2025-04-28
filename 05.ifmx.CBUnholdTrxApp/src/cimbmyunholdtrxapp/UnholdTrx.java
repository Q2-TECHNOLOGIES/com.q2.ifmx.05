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
//import java.time.LocalDate;
import java.util.Date;
import java.util.Random;
//import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import ch.qos.logback.classic.LoggerContext;
import common.Logging;
//import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public class UnholdTrx {
    public static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static ch.qos.logback.classic.Logger logger = loggerContext.getLogger("UnholdTrx");
  
    
    public static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    public static final SimpleDateFormat dateFormat3 = new SimpleDateFormat("dd-MM-yyyy");
    public static final SimpleDateFormat dateFormat4 = new SimpleDateFormat("dd-MM-yyyy");
    public static final SimpleDateFormat dateFormat5 = new SimpleDateFormat("yyyyMMdd");
    public static final String datenow = dateFormat2.format(new Date());    
    public static final String datenow1 = dateFormat3.format(new Date());

    
    public UnholdTrx(){
        
    }
    private static Connection getDBConnection(String db_host, String db_port, String db_name, String db_username, String db_password, String db_connection_driver) {
        Connection conn = null;
        try {
            if("jtds".equals(db_connection_driver)){
                System.out.println("Creating DB Connection using driver "+db_connection_driver);
                logger.info("Creating DB Connection using driver "+db_connection_driver);
                
                Class.forName("net.sourceforge.jtds.jdbc.Driver");
                String db_url = "jdbc:jtds:sqlserver://"+db_host+":"+db_port+"/";
                
                System.out.println("DB Url : "+db_url);
                logger.info("DB Url : "+db_url);
                
                conn = DriverManager.getConnection(db_url+db_name+"/",db_username,db_password);
            } else{
                System.out.println("Creating DB Connection using driver "+db_connection_driver);
                logger.info("Creating DB Connection using driver "+db_connection_driver);
                
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                String db_url = "jdbc:sqlserver://"+db_host+":"+db_port;
                
                System.out.println("DB Url : "+db_url);
                logger.info("DB Url : "+db_url);
                
                conn = DriverManager.getConnection(db_url+";databaseName="+db_name,db_username,db_password);
            }
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Class not found, error: " + cnfe);
            logger.error("Class not found, error: " + cnfe);
            System.exit(0);
        } catch (SQLException e){
            System.out.println(e.getCause());
            logger.error(e.getMessage());
        }
        return conn;
    }
    
    public static int printLog (PrintWriter out, String twi, JSONObject obj, String channel_cd, String url, String responseCode, String responseMessage ){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:PROCESSING TWI : "+twi);
        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:REQUEST MESSAGE : "+obj.toString());

        //cr ngc
        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:CHANNEL CD : "+channel_cd);
        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UNHOLD URL : "+url);
        //end of cr ngc

        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE CODE : "+responseCode);
        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE MESSAGE : "+ responseMessage);

        System.out.println("REQUEST MESSAGE : "+obj.toString());
        //cr ngc
        System.out.println("CHANNEL CD : "+channel_cd);
        System.out.println("UNHOLD URL : "+url);
        //end of cr ngc
        System.out.println("RESPONSE CODE : "+responseCode);
        System.out.println("RESPONSE MESSAGE : "+ responseMessage);
 
        return 0;
    }
    
    public static int unholdAndCloseAlerts(PrintWriter out, Connection conn, String actionId, String twi, String cwi, String alert_custom_attributes_id){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        Integer update_result = 0;
        Integer update_result_twi = 0;
        Integer update_result_cwi = 0;
        Integer update_result_all = 0;
        String status_internal_id = "";
        String status_internal_id_cwi = "";
        String query_status_internal_id = "";
        String query_status_internal_id_cwi = "";
              
        if("Block".equalsIgnoreCase(actionId)){
            query_status_internal_id = "select status_internal_id from acm_md_alert_statuses where status_identifier = 'CCClosedBlock'";
        }else
            query_status_internal_id = "select status_internal_id from acm_md_alert_statuses where status_identifier = 'CCClosedAccept'";
        
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query_status_internal_id);
            while(rs.next()){
                status_internal_id = rs.getString("status_internal_id");
            }
        }catch(SQLException e1){
            //out.println(datenow+"-ERROR GET STATUS_INTERNAL_ID TWI- "+e1.getMessage());
            out.println(timestamp+"-ERROR GET STATUS_INTERNAL_ID TWI- "+e1.getMessage());
        }
        
        try{
            query_status_internal_id_cwi = "SELECT status_internal_id FROM acm_md_alert_statuses WHERE status_identifier = 'CCClosed'";
            Statement stmt_cwi = conn.createStatement();
            ResultSet rs_cwi = stmt_cwi.executeQuery(query_status_internal_id_cwi);
            while(rs_cwi.next()){
                status_internal_id_cwi = rs_cwi.getString("status_internal_id");
            }
        }catch(SQLException e2){
            //out.println(datenow+"-ERROR GET STATUS_INTERNAL_ID CWI- "+e2.getMessage());
            out.println(timestamp+"-ERROR GET STATUS_INTERNAL_ID CWI- "+e2.getMessage());
        }
        
        try{
            String query_update = "update acm_alert_custom_attributes set ci12 = 0 where alert_custom_attributes_id = "+alert_custom_attributes_id;
            //out.println(datenow+"-UPDATE QUERY C12: " + query_update);
            out.println(timestamp+"-UPDATE QUERY C12: " + query_update);
            Statement stmt_update = conn.createStatement();
            update_result = stmt_update.executeUpdate(query_update);
            update_result_all=update_result_all+update_result;
            

            if(update_result == 1){
                //out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : successful");
                out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : successful");
                System.out.println("UPDATE ci12 : successful");
                
                //update p40 and close TWI
                String query_update_unhold_statusTWI = "update alerts set p40 = '"+actionId+"', status_internal_id = "+status_internal_id+" where alert_id = '"+twi+"'";
                //out.println(datenow+"-UPDATE QUERY TWI: " + query_update_unhold_statusTWI);
                out.println(timestamp+"-UPDATE QUERY TWI: " + query_update_unhold_statusTWI);
                Statement stmt_update_unhold_status = conn.createStatement();
                update_result_twi = stmt_update_unhold_status.executeUpdate(query_update_unhold_statusTWI);
                
                if(update_result_twi == 1){
                    //out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 & CLOSE TWI " +twi+ " : successful");
                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 & CLOSE TWI " +twi+ " : successful");
                    System.out.println("UPDATE p40 & CLOSE TWI : successful");
                } else{
                    //out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 & CLOSE TWI " +twi+ " : failed due to record not found");
                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 & CLOSE TWI " +twi+ " : failed due to record not found");
                    System.out.println("UPDATE p40 & CLOSE TWI : failed");
                }
                
                update_result_all=update_result_all+update_result_twi;
                
                //update close CWI
                String query_update_statusCWI = "update alerts set status_internal_id = "+status_internal_id_cwi+" where alert_id = '"+cwi+"'";
                //out.println(datenow+"-UPDATE QUERY CWI: " + query_update_statusCWI);
                out.println(timestamp+"-UPDATE QUERY CWI: " + query_update_statusCWI);
                Statement stmt_update_statusCWI = conn.createStatement();
                update_result_cwi = stmt_update_statusCWI.executeUpdate(query_update_statusCWI);
                
                if(update_result_cwi == 1){
                    //out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE CLOSE CWI " +cwi+ " : successful");
                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE CLOSE CWI " +cwi+ " : successful");
                    System.out.println("UPDATE CLOSE CWI : successful");
                } else{
                    //out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE CLOSE CWI " +cwi+ " : failed due to record not found");
                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE CLOSE CWI " +cwi+ " : failed due to record not found");
                    System.out.println("UPDATE CLOSE CWI : failed");
                }
                update_result_all=update_result_all+update_result_cwi;
            } else{
                //out.println(datenow+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : failed due to record not found");
                out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : failed due to record not found");
                System.out.println("UPDATE ci12 : failed");
            }
        } catch(SQLException e){
            //out.println(datenow+"-ERROR - "+e.getMessage());
            out.println(timestamp+"-ERROR - "+e.getMessage());
        }
        //out.println(datenow+"-COUNT UPDATE - "+update_result_all);
        out.println(timestamp+"-COUNT UPDATE - "+update_result_all);
        return update_result_all;
    }
    
    
    public static String wsCaller(String actionId, String cwi, String twis){
        
        String result = "";
        PropertiesLoader pl = new PropertiesLoader();
         Logging log = new Logging(); 
        log.configLog(pl, logger, loggerContext);
          SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        logger.info("Start run at "+timestamp);
        Common cmn = new Common(pl);
        
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmmss");
//        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        //SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
//        String timestamp = dateFormat.format(new Date());
        int count = 0;
        String temp_trx_type = "";
        String temp_resp_code = "";
        
        Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password, pl.db_connection_driver);
        
        String[] tmp_twis = twis.split(";"); //bentuk twis itu gimana?
        String summaryResponseMessage = "";
        for(String twi : tmp_twis){
            Random rand = new Random();
            int n = rand.nextInt(100000)+1;
            if(conn != null){
                System.out.println("DB Connection established");
                logger.info("DB Connection established");
                try{
                    String query = "SELECT a.p11 as transaction_type, b.csm18 as transaction_key, a.alert_custom_attributes_id, "
                            //cr ngc
                            + "b.cs13 as channel_cd "
                            //end of cr ngc
                            + "FROM alerts a "
                            + "join acm_alert_custom_attributes b on a.alert_custom_attributes_id = b.alert_custom_attributes_id "
                            + "WHERE a.alert_id = '"+twi+"'";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        count+=count;
                        String auditNo = n+sdf.format(new Date())+"";
                        String trxKey = rs.getString("transaction_key");
                        String trxType = rs.getString("transaction_type");
                        String alert_custom_attributes_id = rs.getString("alert_custom_attributes_id");
                        
                        //cr ngc
                        String channel_cd = rs.getString("channel_cd");
                        String url = pl.clicks_unhold_url;
                        //end of cr ngc

                        String action = "HA";
                        if("Block".equalsIgnoreCase(actionId)){
                            action = "HR";
                        }

                        JSONObject obj = cmn.generateJSONObj(auditNo, trxType, trxKey, action);

                        String responseCode = "";
                        String responseMessage = "";
                       
                        //setup log
                        String dateinit = dateFormat5.format(new Date());
                        File file = new File(pl.unhold_log_file_dir+dateinit+"_RB_CIMBMYUnholdTrxApp.log");
                        String lastmodified = dateFormat3.format(file.lastModified());
                    //    if(!lastmodified.equals(datenow1)){
                    //        SimpleDateFormat tmpsdf = new SimpleDateFormat("ddMMyyHH");
                    //        String tmplastmodified = tmpsdf.format(file.lastModified());
                    //        file.renameTo(new File(pl.unhold_log_file_dir+tmplastmodified+"_RB_CIMBMYUnholdTrxApp"+".log"));
                    //    }
                        //end of setup log
                        
                        try{
                            //cr ngc
                            if(channel_cd != null && !"".equals(channel_cd)){
                                if("NGC".equals(channel_cd.substring(0,3))){
                                    url = pl.ngc_unhold_url;
                                }
                            }
                            
                            result = cmn.sendPost(obj.toString(), url);
                            responseMessage = result.substring(result.indexOf(";")+1);
                            responseCode = result.substring(0, result.indexOf(";"));
                            
                            /*if("RLO_18103".equals(twi)){
                                responseCode = "003";
                            }else if("RLO_18104".equals(twi)){
                                responseCode = "004";
                            }else if("RLO_18105".equals(twi)){
                                responseCode = "005";
                            }else if("RLO_18106".equals(twi)){
                                responseCode = "006";
                            }
                            else responseCode = "500";
                            result = "";
                            responseMessage = "test";*/
                            
                            
                            

                            //CR FTL 2022-Dec
                            temp_resp_code = temp_resp_code+responseCode+";";
                            
                            if("FTL0004".equals(trxType)){
                                temp_trx_type = trxType;
                                try(FileWriter fw = new FileWriter(pl.unhold_log_file_dir+dateinit+"_RB_CIMBMYUnholdTrxApp.log", true);
                                        BufferedWriter bw = new BufferedWriter(fw);
                                        PrintWriter out = new PrintWriter(bw)){
                                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:==============");
                                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TRANSACTION TYPE : "+trxType);
                                    if("000".equals(responseCode)){
                                        printLog(out, twi, obj, channel_cd, url, responseCode, responseMessage );
                                        unholdAndCloseAlerts(out, conn, actionId, twi, cwi, alert_custom_attributes_id);

                                        System.out.println("Unhold Transaction(s) successful");

                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);
                                        System.out.println("TIMESTAMP : "+timestamp);
                                        
                                        summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" successful\n";
                                        
                                    }else if("005".equals(responseCode)){
                                        printLog(out, twi, obj, channel_cd, url, responseCode, responseMessage );
                                        unholdAndCloseAlerts(out, conn, "Accept", twi, cwi, alert_custom_attributes_id);

                                        System.out.println("Unhold Transaction(s) successful");

                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);
                                        System.out.println("TIMESTAMP : "+timestamp);
                                        
                                        summaryResponseMessage += "Previous unhold is successful. Please inform customer to proceed. If block is required, please perform maintenance via Clicks or Novus Support Centre.  \n"+
                                                                  "["+twi+"] Transaction Key "+trxKey+" successful\n";
                                        
                                    }else if("006".equals(responseCode)){
                                        printLog(out, twi, obj, channel_cd, url, responseCode, responseMessage );
                                        unholdAndCloseAlerts(out, conn, "Block", twi, cwi, alert_custom_attributes_id);

                                        System.out.println("Unhold Transaction(s) successful");

                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);
                                        System.out.println("TIMESTAMP : "+timestamp);
                                        
                                        summaryResponseMessage += "Previous unhold Block is successful. Please inform customer to perform the FTL again. \n"+
                                                                  "["+twi+"] Transaction Key "+trxKey+" successful\n";
                                        
                                    }else if("003".equals(responseCode)||"004".equals(responseCode)){
                                        printLog(out, twi, obj, channel_cd, url, responseCode, responseMessage );
                                        
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);
                                        System.out.println("TIMESTAMP : "+timestamp);
                                        
                                        summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" unsuccessful.\n";
                                        
                                    }else {
                                        printLog(out, twi, obj, channel_cd, url, responseCode, responseMessage );
                                        
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);
                                        System.out.println("TIMESTAMP : "+timestamp);
                                        
                                        summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" failed. Error code :"+responseCode+ "\n";
                                    }
                                } catch (IOException e) {
                                            System.out.println(e.getCause());
                                }
                            }
                            //END of CR FTL 2022-Dec
                            
                            else {
                                if("000".equals(responseCode)){
                                    try(FileWriter fw = new FileWriter(pl.unhold_log_file_dir+dateinit+"_RB_CIMBMYUnholdTrxApp.log", true);
                                        BufferedWriter bw = new BufferedWriter(fw);
                                        PrintWriter out = new PrintWriter(bw)){
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:==============");
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:PROCESSING TWI : "+twis);
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:REQUEST MESSAGE : "+obj.toString());

                                        //cr ngc
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:CHANNEL CD : "+channel_cd);
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UNHOLD URL : "+url);
                                        //end of cr ngc

                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE CODE : "+responseCode);
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE MESSAGE : "+ "Unhold Transaction(s) successful");

                                        System.out.println("REQUEST MESSAGE : "+obj.toString());
                                        //cr ngc
                                        System.out.println("CHANNEL CD : "+channel_cd);
                                        System.out.println("UNHOLD URL : "+url);
                                        //end of cr ngc
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
                                                out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : successful");
                                                System.out.println("UPDATE ci12 : successful");
                                                String query_update_unhold_status = "update alerts set p40 = '"+actionId+"' where alert_id = '"+twi+"'";
                                                Statement stmt_update_unhold_status = conn.createStatement();
                                                update_result = stmt_update_unhold_status.executeUpdate(query_update_unhold_status);
                                                if(update_result == 1){
                                                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 : successful");
                                                    System.out.println("UPDATE p40 : successful");
                                                } else{
                                                    out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE p40 : failed due to record not found");
                                                    System.out.println("UPDATE p40 : failed");
                                                }
                                            } else{
                                                out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UPDATE ci12 : failed due to record not found");
                                                System.out.println("UPDATE ci12 : failed");
                                            }
                                        } catch(SQLException e){
                                            out.println(timestamp+"-ERROR - "+e.getMessage());
                                        }

                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);
                                        System.out.println("TIMESTAMP : "+timestamp);
                                    } catch (IOException e) {
                                        System.out.println(e.getCause());
                                    }
                                    summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" successful\n";
                            } else
                                {
                                    try(FileWriter fw = new FileWriter(pl.unhold_log_file_dir+dateinit+"_RB_CIMBMYUnholdTrxApp.log", true);
                                        BufferedWriter bw = new BufferedWriter(fw);
                                        PrintWriter out = new PrintWriter(bw)){

                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:==============");
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:REQUEST MESSAGE : "+obj.toString());

                                        //cr ngc
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:CHANNEL CD : "+channel_cd);
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:UNHOLD URL : "+url);
                                        //end of cr ngc

                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE CODE : "+responseCode);
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:RESPONSE MESSAGE : "+ responseMessage);
                                        out.println(timestamp+"-INFO -cimbmyunholdtrxapp.UnholdTrx:TIMESTAMP : "+timestamp);

                                        System.out.println("REQUEST MESSAGE : "+obj.toString());
                                        //cr ngc
                                        System.out.println("CHANNEL CD : "+channel_cd);
                                        System.out.println("UNHOLD URL : "+url);
                                        //end of cr ngc
                                        System.out.println("RESPONSE CODE : "+responseCode);
                                        System.out.println("RESPONSE MESSAGE : "+ responseMessage);
                                        System.out.println("TIMESTAMP : "+timestamp);
                                    } catch (IOException e) {
                                        System.out.println(e.getCause());
                                    }
                                    summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" failed. Error code : 500\n";
                                }
                            }
                            
                            
                        } catch (Exception e){
                            try(FileWriter fw = new FileWriter(pl.unhold_log_file_dir+dateinit+"_RB_CIMBMYUnholdTrxApp.log", true);
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
                            //cr ngc
                            System.out.println("CHANNEL CD : "+channel_cd);
                            System.out.println("UNHOLD URL : "+url);
                            //end of cr ngc
                            System.out.println("RESPONSE CODE : "+responseCode);
                            System.out.println("RESPONSE MESSAGE : "+ responseMessage);
                            System.out.println("TIMESTAMP : "+timestamp);
                            //return "Unhold transaction(s) failed with Error Code : "+responseCode+". Please contact your system administrator.";
                            summaryResponseMessage += "["+twi+"] Transaction Key "+trxKey+" failed. Error code "+responseCode+"\n";
                        }
                    }
                } catch(SQLException e){
                    System.out.println(e.getCause());
                    logger.error(e.getMessage());
                    return "SQL Exception : "+e.getCause();
                }
            } else{
                System.out.println("Unable to established DB Connection!!!");
                logger.error("Unable to established DB Connection!!!");
            }
        }
        //System.out.println(temp_resp_code);
        //CR FTL 2022-Dec
        if("FTL0004".equals(temp_trx_type) && !(temp_resp_code.contains("000;")||temp_resp_code.contains("005;")||temp_resp_code.contains("006;"))){
            if(temp_resp_code.contains("003;")||temp_resp_code.contains("004;"))
                summaryResponseMessage = "Please release the newest alert or advise customer to perform FTL again. " + summaryResponseMessage;
            else 
                summaryResponseMessage = "Please retry again. " + summaryResponseMessage;
        }
        
        String timestamp1 = dateFormat.format(new Date());
        logger.info("Finish run at "+timestamp1);
        //END of CR FTL 2022-Dec
        
        return summaryResponseMessage;
    }
    
    public static void main(String[] args) {
        UnholdTrx u = new UnholdTrx();
//        pl = new PropertiesLoader("Z:\ACTIMIZE\Batch\app.properties");
       
      
        System.out.println(u.wsCaller(args[0],"cwi",args[1]));
        //System.out.println("Summary Response = " + u.wsCaller("Accept","WI_0000020025", "WI_0000020026"));
    }
}
