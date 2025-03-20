package cimbalertnotificationapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import model.EmailObject;
import model.SMSObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import model.MessageContent;
//import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author firman
 */
public class CIMBAlertNotificationApp {
    //final static Logger logger = Logger.getLogger(CIMBAlertNotificationApp.class);
    public static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static ch.qos.logback.classic.Logger logger = loggerContext.getLogger("CIMBAlertNotificationApp");
    
    private static Connection getDBConnection(String db_host, String db_port, String db_name, String db_username, String db_password) {
        Connection conn = null;
        try {
            //Class.forName("net.sourceforge.jtds.jdbc.Driver");
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            //String db_url = "jdbc:jtds:sqlserver://"+db_host+":"+db_port+"/";
            String db_url = "jdbc:sqlserver://"+db_host+":"+db_port+"";
            //conn = DriverManager.getConnection(db_url+db_name+"/",db_username,db_password);
            conn = DriverManager.getConnection(db_url+";databaseName="+db_name,db_username,db_password);
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
    
    public static ArrayList<String> getPhoneNumber(String status, PropertiesLoader pl){
        String bu = "FraudAnalystTeamLeader";
        if("IFM - Ready".equals(status)){
            bu = "FraudAnalystTeamLeader";
        } else if("Assigned".equals(status)){
            bu = "FraudAnalyst";
        }
        
        ArrayList<String> phone_list = new ArrayList<String>();
        
        Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password);
        if(conn != null){
            try{
                
                    logger.info("Start running query for get phone number data");
                    
                String query = "SELECT user_identifier , B.ls2 as EmailAddress, B.ls3 as PhoneNumber " +
                                "FROM acm_users A " +
                                "INNER JOIN acm_internal_list_items B ON A.user_identifier = substring(B.ls1, 0, CHARINDEX(' ', B.ls1)) " +
                                "INNER JOIN acm_md_internal_lists C ON B.list_internal_id = C.list_internal_id " +
                                "INNER JOIN acm_md_business_units D ON A.bu_internal_id = D.bu_internal_id " +
                                "WHERE is_active = 1 AND type = 0 " +
                                "AND D.bu_identifier = '"+bu+"' "+
                                "AND C.list_identifier = 'FAEmail'";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    String phone_number = rs.getString("PhoneNumber");
                    phone_list.add(phone_number);
                    logger.info("Finish running query for get phone number data from table acm_users");
                }
            } catch(SQLException e){
                System.out.println(e.getCause());
                logger.error(e.getMessage());

            }
        }
        return phone_list;
    }
    
    public static String getDBPartitionKey(PropertiesLoader pl){
        String partitions = "";
        Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_idb_name, pl.db_idb_username, pl.db_idb_password);
        Connection rcm_conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password);
        try{
             logger.error("Start running query for get partition number in IMPL_TRX_TBL_PARTITIONS");
            String query_check_partition= "SELECT PARTITION_NUMBER FROM IMPL_TRX_TBL_PARTITIONS WHERE TRX_DATE = CONVERT(DATE, GETDATE())";
            Statement stmt = rcm_conn.createStatement();
            ResultSet rs = stmt.executeQuery(query_check_partition);
            logger.info("Finish running query for get partition number in IMPL_TRX_TBL_PARTITIONS");
            
            while (rs.next()) {
                partitions = rs.getString("PARTITION_NUMBER");
                return partitions;
            }
            
            logger.info("Start running query for get partition in sys.partition");
            
            String query = "select  partition_number AS partition "+
                            "from sys.partitions p "+
                            "join sys.partition_range_values prv on p.partition_number = prv.boundary_id "+
                            "join sys.partition_functions pf on pf.function_id = prv.function_id "+
                            "join sys.indexes i on i.index_id = p.index_id "+
                            "where object_name(p.object_id) = 'FF_ALL_TRANSACTIONS' "+
                            "and pf.name =  'PFN_FF_ALL_TRANSACTIONS' "+
                            "and i.name = 'PKFF_ALL_TRANSACTIONS' "+
                            "and convert(date, prv.value) = CONVERT(DATE, GETDATE()) "+ //get yesterday partition
                            "UNION ALL "+
                            "select  partition_number AS partition "+
                            "from sys.partitions p "+
                            "join sys.partition_range_values prv on p.partition_number = prv.boundary_id "+
                            "join sys.partition_functions pf on pf.function_id = prv.function_id "+
                            "join sys.indexes i on i.index_id = p.index_id "+
                            "where object_name(p.object_id) = 'FF_ALL_TRANSACTIONS' "+
                            "and pf.name =  'PFN_FF_ALL_TRANSACTIONS' "+
                            "and i.name = 'PKFF_ALL_TRANSACTIONS' "+
                            "and convert(date, prv.value) = CONVERT(DATE, GETDATE()+1) "; //get running day partition

            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                partitions += rs.getString("partition")+",";
            logger.info("Finish running query for insert partition in IMPL_TRX_TBL_PARTITIONS from sys.partition");
            }
            
            partitions = partitions.substring(0, partitions.lastIndexOf(","));
            logger.info("Start running query for insert partition in IMPL_TRX_TBL_PARTITIONS");
            String query_insert_partition = "INSERT INTO IMPL_TRX_TBL_PARTITIONS VALUES (CONVERT(DATE, GETDATE()), '"+partitions+"')";
            stmt = rcm_conn.createStatement();
            Integer result = stmt.executeUpdate(query_insert_partition);
            logger.info("Finish running query for insert partition in IMPL_TRX_TBL_PARTITIONS");
            
            
        } catch(SQLException e){
            System.out.println("Error when retrieving partition number");
            logger.error(e.getMessage());
            

        }
        
        return partitions;
    }
    
    public static ArrayList<String> getEmail(String status, PropertiesLoader pl){
        ArrayList<String> email_list = new ArrayList<String>();
        String bu = "";
        if("IFM - Ready".equals(status)){
            email_list.add(pl.email_group_recipient);
            return email_list;
        } else if("Assigned".equals(status)){
            bu = "FraudAnalyst";
        }
        
        Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password);
        if(conn != null){
            try{
            logger.info("Start running query for get email address data from acm users");
                String query = "SELECT user_identifier , B.ls2 as EmailAddress, B.ls3 as PhoneNumber " +
                                "FROM acm_users A " +
                                "INNER JOIN acm_internal_list_items B ON A.user_identifier = substring(B.ls1, 0, CHARINDEX(' ', B.ls1)) " +
                                "INNER JOIN acm_md_internal_lists C ON B.list_internal_id = C.list_internal_id " +
                                "INNER JOIN acm_md_business_units D ON A.bu_internal_id = D.bu_internal_id " +
                                "WHERE is_active = 1 AND type = 0 " +
                                "AND D.bu_identifier = '"+bu+"' "+
                                "AND C.list_identifier = 'FAEmail'";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    String email_address = rs.getString("EmailAddress");
                    email_list.add(email_address);
            logger.info("Finish running query for get email address data from acm users");
            
                }
            } catch(SQLException e){
                System.out.println(e.getCause());
                logger.error(e.getMessage());

            }
        }
        try{
            if(conn != null){
                conn.close();
            }
        } catch(SQLException e){
            System.out.println("Failed to Close DB Connection");
            logger.info("Failed to Close DB Connection");

        }
        return email_list;
    }
    
    public static boolean isSendNotification(String status, PropertiesLoader pl, String cwi, String table, String recipient, String filter_lastDate, String filter_currDate, String partitions){
        Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_idb_name, pl.db_idb_username, pl.db_idb_password);
        if(conn != null){
            try{
            logger.info("Start running query for checking if eligible to send "+table);
                String query = "select distinct d.RECIPIENTS  " + //aa.item_id AS 'CWI', aa.child_id AS 'TWI', b.status_internal_id, 
                                "from RCM.dbo.v_items_parent_child_relations aa " +
                                "join RCM.dbo.alerts a on aa.item_id = a.alert_id " +
                                //"join RCM.dbo.acm_md_alert_statuses b on a.status_internal_id = b.status_internal_id and a.status_internal_id = 1816 " + //assigned alert
                                "join RCM.dbo.acm_md_alert_statuses b on a.status_internal_id = b.status_internal_id and b.status_name = 'Assigned' " + //assigned alert
                                "join RCM.dbo.alerts c on aa.child_id = c.alert_id "+
                                "join IDB.DATA_IDB.FF_ALL_TRANSACTIONS trx ON c.p50 = trx.ACTIMIZE_TRANSACTION_IDENTITY AND $PARTITION.PFN_FF_ALL_TRANSACTIONS(TRX_NORMALIZED_DATETIME) IN ("+partitions+") "+
                                "join RCM.dbo.IMPL_"+table+"_ALERT_NOTIFICATION d ON d.TRX_DATE = trx.TRX_NORMALIZED_DATETIME AND d.PARTY_ID = trx.PARTY_KEY AND d.SEND_STATUS = 1 "+
                                "WHERE d.RECIPIENTS = '"+recipient+"'";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    conn.close();
                    logger.info("Finish running query for checking if eligible to send "+table+". Notification will not be send");
                    return false;
                }
            } catch(SQLException e){
                System.out.println(e.getCause());
            }
        }
        try{
            if(conn != null){
                conn.close();
            }
        } catch(SQLException e){
            System.out.println("Failed to Close DB Connection");
        }
        logger.info("Finish running query for checking if eligible to send "+table+". Notification will be send");
        return true;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        //get date
        SimpleDateFormat dateSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //System.out.println(dateSDF.format(new Date()));
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        //System.out.println(dateSDF.format(cal.getTime()));
        
        String filter_currDate = dateSDF.format(new Date());
        String filter_lastDate = dateSDF.format(cal.getTime());
        //end of get date
        
        
        PropertiesLoader pl = new PropertiesLoader(args[0]);
        MessageBuilder mb = new MessageBuilder();
        
        String partitions = getDBPartitionKey(pl);
        System.out.println("Table Partition Found = "+partitions);
        
        //send sms process
        SimpleDateFormat sdf1 = new SimpleDateFormat("ddMMyyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HHmmss");
        Date currDate = new Date();
        String strDate = sdf1.format(currDate)+"_"+sdf2.format(currDate);
        Connection idb_conn = getDBConnection(pl.db_host, pl.db_port, pl.db_idb_name, pl.db_idb_username, pl.db_idb_password);
        Connection rcm_conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password);
        if(rcm_conn != null && idb_conn != null){
            try{
                logger.info("Start running query checking transactions for sending SMS notification");
                String query = "SELECT DISTINCT aa.item_id AS 'CWI', alstat.status_name AS STATUS_ID, ac.owner_identifier " +
                                "from RCM.dbo.v_items_parent_child_relations aa " +
                                "JOIN RCM.dbo.alerts ab on aa.child_id = ab.alert_id " +
                                "JOIN IDB.DATA_IDB.FF_ALL_TRANSACTIONS trx ON ab.p50 = trx.ACTIMIZE_TRANSACTION_IDENTITY AND $PARTITION.PFN_FF_ALL_TRANSACTIONS(TRX_NORMALIZED_DATETIME) IN ("+partitions+") "+
                                "INNER JOIN RCM.dbo.IMPL_SMS_ALERT_NOTIFICATION C ON C.TRX_DATE = trx.TRX_NORMALIZED_DATETIME AND C.PARTY_ID = trx.PARTY_KEY " +
                                "INNER JOIN RCM.dbo.alerts ac ON aa.item_id = ac.alert_id " +
                                "INNER JOIN RCM.dbo.acm_md_alert_statuses alstat ON alstat.status_internal_id = ac.status_internal_id "+
                                "WHERE SEND_STATUS=0 AND SEND_COUNT < 4 ";
                Statement stmt = idb_conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                logger.info("Finish running query checking transactions for sending SMS notification");

                
                while (rs.next()) {
                    String cwi = rs.getString("CWI");
                    logger.info("Processing CWI : "+cwi);

                    SMSObject smsObj = new SMSObject();
                    smsObj.setSms_url(pl.sms_url);
                    smsObj.setSms_user(pl.sms_user);
                    smsObj.setPriority("1");
                    smsObj.setTar_mode("text");
                    smsObj.setTimestamp(strDate);
                    
                    String query_get_twi = "select aa.item_id AS 'CWI', aa.child_id AS 'TWI', C.* " +
                                            "from RCM.dbo.v_items_parent_child_relations aa " +
                                            "join RCM.dbo.alerts ab on aa.child_id = ab.alert_id " +
                                            "join IDB.DATA_IDB.FF_ALL_TRANSACTIONS trx ON ab.p50 = trx.ACTIMIZE_TRANSACTION_IDENTITY AND $PARTITION.PFN_FF_ALL_TRANSACTIONS(TRX_NORMALIZED_DATETIME) IN ("+partitions+") "+
                                            "INNER JOIN RCM.dbo.IMPL_SMS_ALERT_NOTIFICATION C ON C.TRX_DATE = trx.TRX_NORMALIZED_DATETIME AND C.PARTY_ID = trx.PARTY_KEY " +
                                            "WHERE aa.item_id = '"+cwi+"'";
                    
                    Statement stmt_twi = idb_conn.createStatement();
                    ResultSet rs_twi = stmt_twi.executeQuery(query_get_twi);
                    ArrayList<MessageContent> listMessage = new ArrayList<MessageContent>();
                    String trx_key_for_update_status = " IN ( ";
                    Integer tmp_counter = 0;
                    
                    while (rs_twi.next()) {
                        MessageContent msgContent = new MessageContent();
                        msgContent.setTrx_date(rs_twi.getString("TRX_DATE"));
                        msgContent.setAlert_id(rs_twi.getString("CWI"));
                        msgContent.setParty_id(rs_twi.getString("PARTY_ID"));
                        msgContent.setCust_name(rs_twi.getString("CUST_NAME"));
                        msgContent.setTrx_type(rs_twi.getString("TRX_TYPE"));
                        String tmp_amount = rs_twi.getString("TRX_AMOUNT");
                        if("".equals(tmp_amount) || tmp_amount == null){
                            tmp_amount = "0";
                        } else{
                            tmp_amount = (Double.valueOf(tmp_amount).longValue())+"";
                        }
                        msgContent.setTrx_amt(tmp_amount);
                        
                        msgContent.setResponse(pl.responseMap.get(rs_twi.getString("RESPONSE")));
                        msgContent.setScore(rs_twi.getString("SCORE"));
                        msgContent.setChannel_type(rs_twi.getString("CHANNEL_TYPE"));
                        listMessage.add(msgContent);
                        
                        if(tmp_counter == 0){
                            trx_key_for_update_status += "'"+rs_twi.getString("TRX_KEY")+"'";
                        } else{
                            trx_key_for_update_status += ",'"+rs_twi.getString("TRX_KEY")+"'";
                        }
                        tmp_counter++;
                        
                    }
                    trx_key_for_update_status += " )";
                    smsObj.setListMessage(listMessage);
                    smsObj.setTar_msg(mb.smsMessageBuilder(listMessage));

                    SMSSenderApp smsSender = new SMSSenderApp();
                    ArrayList<String> phoneNumberList = getPhoneNumber(rs.getString("STATUS_ID"), pl);
                    
                    if(!isSendNotification(rs.getString("STATUS_ID"),pl,cwi, "SMS", phoneNumberList.get(0), filter_lastDate, filter_currDate, partitions)){
                        continue;
                    }
                    
                    String result ="";
                    String phoneNumber = phoneNumberList.get(0);
                    smsObj.setTar_num(phoneNumber);
                    result = smsSender.sendSMS(smsObj);
                    //System.out.println("send sms");
                    
                    String update_query = "";
                    
                    SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String timestamp = dtf.format(new Date());
                    String tmp_recipients = smsObj.getTar_num();

                    if(result.equals("1")){
                        update_query = "UPDATE RCM.dbo.IMPL_SMS_ALERT_NOTIFICATION "
                                + "SET SEND_STATUS = 1,SEND_COUNT = SEND_COUNT+1, TIMESTAMP = '"+timestamp+"', RECIPIENTS='"+tmp_recipients+"' WHERE SEND_STATUS=0 AND TRX_KEY "+trx_key_for_update_status;
                    } else{
                        update_query = "UPDATE RCM.dbo.IMPL_SMS_ALERT_NOTIFICATION "
                                + "SET SEND_COUNT = SEND_COUNT+1, TIMESTAMP = '"+timestamp+"', RECIPIENTS='"+tmp_recipients+"' WHERE SEND_STATUS=0 AND TRX_KEY "+trx_key_for_update_status;
                    }
                    Statement update_stmt = rcm_conn.createStatement();
                    int update_result = update_stmt.executeUpdate(update_query);
                }
            } catch (SQLException e){
                System.out.println("SQL EXCEPTION : ERROR "+e.getMessage());
                logger.error("SQL EXCEPTION : ERROR "+e.getMessage());

            }
        }
       
        if(rcm_conn != null && idb_conn != null){
            try{
                //query get cwi from email alert notification table
                 logger.info("Start running query checking transactions for sending EMAIL notification");
                String query = "select DISTINCT aa.item_id AS 'CWI', alstat.status_name AS STATUS_ID, ac.owner_identifier " +
                                "from RCM.dbo.v_items_parent_child_relations aa " +
                                "join RCM.dbo.alerts ab on aa.child_id = ab.alert_id " +
                                "join IDB.DATA_IDB.FF_ALL_TRANSACTIONS trx ON ab.p50 = trx.ACTIMIZE_TRANSACTION_IDENTITY AND $PARTITION.PFN_FF_ALL_TRANSACTIONS(TRX_NORMALIZED_DATETIME) IN ("+partitions+") "+
                                "INNER JOIN RCM.dbo.IMPL_EMAIL_ALERT_NOTIFICATION C ON C.TRX_DATE = trx.TRX_NORMALIZED_DATETIME AND C.PARTY_ID = trx.PARTY_KEY " +
                                "INNER JOIN RCM.dbo.alerts ac ON aa.item_id = ac.alert_id " +
                                "INNER JOIN RCM.dbo.acm_md_alert_statuses alstat ON alstat.status_internal_id = ac.status_internal_id "+
                                "WHERE SEND_STATUS=0 AND SEND_COUNT < 4 ";
                Statement stmt = idb_conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                logger.info("Finish running query checking transactions for sending EMAIL notification");

                ArrayList<EmailObject> listEmailObject = new ArrayList<EmailObject>();
                while (rs.next()) {
                    String cwi = rs.getString("CWI");
                    logger.info("Processing CWI : "+cwi);

                    //send email process
                    EmailObject emailObj = new EmailObject();
                    emailObj.setSmtp_host(pl.smtp_host);
                    emailObj.setSmtp_port(pl.smtp_port);
                    emailObj.setEmail_user(pl.email_username);
                    emailObj.setEmail_password(pl.email_password);
                    emailObj.setEmail_subject("RCM Alert Notification");
                    
                    String query_get_twi = "select aa.item_id AS 'CWI', aa.child_id AS 'TWI', C.* " +
                                            "from RCM.dbo.v_items_parent_child_relations aa " +
                                            "join RCM.dbo.alerts ab on aa.child_id = ab.alert_id " +
                                            "join IDB.DATA_IDB.FF_ALL_TRANSACTIONS trx ON ab.p50 = trx.ACTIMIZE_TRANSACTION_IDENTITY AND $PARTITION.PFN_FF_ALL_TRANSACTIONS(TRX_NORMALIZED_DATETIME) IN ("+partitions+") "+
                                            "INNER JOIN RCM.dbo.IMPL_EMAIL_ALERT_NOTIFICATION C ON C.TRX_DATE = trx.TRX_NORMALIZED_DATETIME AND C.PARTY_ID = trx.PARTY_KEY " +
                                            "WHERE aa.item_id = '"+cwi+"'";
                    Statement stmt_twi = idb_conn.createStatement();
                    ResultSet rs_twi = stmt_twi.executeQuery(query_get_twi);
                    ArrayList<MessageContent> listMessage = new ArrayList<MessageContent>();
                    String trx_key_for_update_status = " IN ( ";
                    Integer tmp_counter = 0;
                    while (rs_twi.next()) {
                        MessageContent msgContent = new MessageContent();
                        msgContent.setTrx_date(rs_twi.getString("TRX_DATE"));
                        msgContent.setAlert_id(rs_twi.getString("CWI"));
                        msgContent.setParty_id(rs_twi.getString("PARTY_ID"));
                        msgContent.setCust_name(rs_twi.getString("CUST_NAME"));
                        msgContent.setTrx_type(rs_twi.getString("TRX_TYPE"));
                        String tmp_amount = rs_twi.getString("TRX_AMOUNT");
                        if("".equals(tmp_amount) || tmp_amount == null){
                            tmp_amount = "0";
                        } else{
                            tmp_amount = (Double.valueOf(tmp_amount).longValue())+"";
                        }
                        msgContent.setTrx_amt(tmp_amount);
                        msgContent.setResponse(pl.responseMap.get(rs_twi.getString("RESPONSE")));
                        msgContent.setScore(rs_twi.getString("SCORE"));
                        msgContent.setChannel_type(rs_twi.getString("CHANNEL_TYPE"));
                        listMessage.add(msgContent);
                        
                        if(tmp_counter == 0){
                            trx_key_for_update_status += "'"+rs_twi.getString("TRX_KEY")+"'";
                        } else{
                            trx_key_for_update_status += ",'"+rs_twi.getString("TRX_KEY")+"'";
                        }
                        tmp_counter++;
                        
                    }
                    trx_key_for_update_status += " )";
                    
                    emailObj.setList_message(listMessage);
                    emailObj.setEmail_message(mb.emailMessageBuilder(listMessage));

                    ArrayList<String> list_recipients = new ArrayList<String>();
                    
                    ArrayList<String> emailAddressList = getEmail(rs.getString("STATUS_ID"), pl);
                    
                    if(!isSendNotification(rs.getString("STATUS_ID"),pl,cwi, "EMAIL", emailAddressList.get(0)+";", filter_lastDate, filter_currDate, partitions)){
                        continue;
                    }
                    
                    for(String emailAddress : emailAddressList){
                        list_recipients.add(emailAddress);
                    }
                    emailObj.setList_recipients(list_recipients);
                    listEmailObject.add(emailObj);
                    
                    EmailSenderApp emailSender = new EmailSenderApp();
                    boolean result = emailSender.sendEmail(emailObj);
                    
                    String update_query = "";
                    SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String timestamp = dtf.format(new Date());
                    String tmp_recipients = "";
                    for(int i=0; i<list_recipients.size(); i++){
                        tmp_recipients = list_recipients.get(i)+"; ";
                    }
                    if(result){
                        update_query = "UPDATE RCM.dbo.IMPL_EMAIL_ALERT_NOTIFICATION "
                                + "SET SEND_STATUS = 1,SEND_COUNT = SEND_COUNT+1, TIMESTAMP = '"+timestamp+"', RECIPIENTS='"+tmp_recipients+"' WHERE SEND_STATUS = 0 AND TRX_KEY "+trx_key_for_update_status;
                    } else{
                        update_query = "UPDATE RCM.dbo.IMPL_EMAIL_ALERT_NOTIFICATION "
                                + "SET SEND_COUNT = SEND_COUNT+1, TIMESTAMP = '"+timestamp+"', RECIPIENTS='"+tmp_recipients+"' WHERE SEND_STATUS=0 AND TRX_KEY "+trx_key_for_update_status;
                    }
                    Statement update_stmt = rcm_conn.createStatement();
                    int update_result = update_stmt.executeUpdate(update_query);
                }
            } catch (SQLException e){
                System.out.println("SQL EXCEPTION : ERROR "+e.getMessage());
                logger.error("SQL EXCEPTION : ERROR "+e.getMessage());

                
            }
        }
        
        try{
            if(rcm_conn != null){
                rcm_conn.close();
            }
            
            if(idb_conn != null){
                idb_conn.close();
            }
        } catch (SQLException e){
            System.out.println("SQL EXCEPTION : ERROR "+e.getMessage());
            logger.error("SQL EXCEPTION : ERROR "+e.getMessage());

        }
    }
}
