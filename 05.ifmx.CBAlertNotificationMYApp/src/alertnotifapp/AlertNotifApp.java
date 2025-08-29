package alertnotifapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.LoggerFactory;

import alertnotifapp.model.EmailObject;
import alertnotifapp.model.MessageBuilder;
import alertnotifapp.model.MessageContent;
import ch.qos.logback.classic.LoggerContext;
import alertnotifapp.common.Logging; // Import kelas Logging
import ch.qos.logback.classic.Logger;

public class AlertNotifApp {
    // Logger initialization at class level
    public static LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    public static ch.qos.logback.classic.Logger logger = loggerContext.getLogger("CIMBAlertNotificationApp");
    private static Connection getDBConnection(String db_host, String db_port, String db_name, 
                                             String db_username, String db_password) {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            String db_url = "jdbc:postgresql://" + db_host + ":" + db_port + "/" + db_name;
            conn = DriverManager.getConnection(db_url, db_username, db_password);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Database connection error: " + e.getMessage());
        }
        return conn;
    }
    public static String getDBPartitionKey(PropertiesLoader pl) {
    String partitions = "";
    Connection idbConn = getDBConnection(pl.db_host, pl.db_port, pl.db_idb_name,
                                        pl.db_username, pl.db_password);
    Connection rcmConn = getDBConnection(pl.db_host, pl.db_port, pl.db_name,
                                         pl.db_username, pl.db_password);

    if (idbConn == null || rcmConn == null) {
        logger.error("Failed to establish database connections for partition keys");
        return partitions;
    }

    try {
        logger.info("Start running query for get partition number in actone.impl_trx_tbl_partitions");
        String queryCheckPartition =
            "SELECT partition_number " +
            "FROM actone.impl_trx_tbl_partitions " +
            "WHERE trx_date = CURRENT_DATE";

        Statement stmt = rcmConn.createStatement();
        ResultSet rs = stmt.executeQuery(queryCheckPartition);
        logger.info("Finish running query for get partition number in impl_trx_tbl_partitions");

        if (rs.next()) {
            partitions = rs.getString("partition_number");
            return partitions;
        }

        logger.info("Start running query for get partitions from idb_data_user.ff_all_transactions");

        String query =
            "SELECT child.relname AS partition_name " +
            "FROM pg_inherits " +
            "JOIN pg_class parent ON pg_inherits.inhparent = parent.oid " +
            "JOIN pg_class child ON pg_inherits.inhrelid = child.oid " +
            "JOIN pg_namespace nmsp_parent ON nmsp_parent.oid = parent.relnamespace " +
            "WHERE parent.relname = 'ff_all_transactions' " +
            "AND nmsp_parent.nspname = 'idb_data_user' " +
            "AND ( " +
            "  child.relname LIKE '%p' || to_char(CURRENT_DATE, 'DD_MM_YYYY') || '%' " +
            "  OR child.relname LIKE '%p' || to_char(CURRENT_DATE - INTERVAL '1 day', 'DD_MM_YYYY') || '%' " +
            ") " +
            "ORDER BY child.relname";

        stmt = idbConn.createStatement();
        rs = stmt.executeQuery(query);

        StringBuilder partitionBuilder = new StringBuilder();
        while (rs.next()) {
            String partitionName = rs.getString("partition_name");
            // ambil nomor partisi dari nama (misal ff_all_transactions_p02_06_2025 → 02)
            String partitionNum = partitionName.replaceAll(".*_p(\\d+)_.*", "$1");
            partitionBuilder.append(partitionNum).append(",");
        }

        logger.info("Finish running query for get partitions from ff_all_transactions");

        if (partitionBuilder.length() > 0) {
            partitions = partitionBuilder.substring(0, partitionBuilder.length() - 1);

            logger.info("Start inserting into actone.impl_trx_tbl_partitions");
            String queryInsertPartition =
                "INSERT INTO actone.impl_trx_tbl_partitions (trx_date, partition_number) " +
                "VALUES (CURRENT_DATE, ?)";
            try (PreparedStatement pstmt = rcmConn.prepareStatement(queryInsertPartition)) {
                pstmt.setString(1, partitions);
                int result = pstmt.executeUpdate();
                logger.info("Inserted partition record: " + result);
            }
            logger.info("Finish inserting into actone.impl_trx_tbl_partitions");
        }
    } catch (SQLException e) {
        logger.error("Error when retrieving partition number: " + e.getMessage());
    } finally {
        try {
            if (idbConn != null) idbConn.close();
            if (rcmConn != null) rcmConn.close();
        } catch (SQLException e) {
            logger.error("Error closing connections: " + e.getMessage());
        }
    }
//    HAPUS SEBELUM DEPLOY
     if (partitions.isEmpty()) {
         logger.warn("Partition is empty, using dummy for testing");
         partitions = "18"; 
     }
    return partitions;
}
    public static ArrayList<String> getEmailRecipients(String status, PropertiesLoader pl) {
    ArrayList<String> emailList = new ArrayList<>();

    if (pl.alert_ready_step_id.equals(status)) {
        // case: ready → kirim ke group email
        emailList.add(pl.email_group_recipient);
        return emailList;
    } else if (pl.alert_assigned_status_id.equals(status)) {
        // case: assigned → lookup BU di DB
        String bu = pl.bu_assigned_alert;
        String listIdentifier = pl.list_identifier_email;  
        Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password);
        if (conn != null) {
            try {
                String query = "SELECT user_identifier, b.ls2 AS email_address " +
                               "FROM acm_users a " +
                               "JOIN acm_internal_list_items b ON a.user_identifier = split_part(b.ls1, ' ', 1) " +
                               "JOIN acm_md_internal_lists c ON b.list_internal_id = c.list_internal_id " +
                               "JOIN acm_md_business_units d ON a.bu_internal_id = d.bu_internal_id " +
                               "WHERE a.is_active = 1 AND a.type = 0 " +
                               "AND d.bu_identifier = '"+bu+"' "+
                               "AND c.list_identifier = '"+listIdentifier+"' ";

                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, bu);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    emailList.add(rs.getString("email_address"));
                }

            } catch (SQLException e) {
                logger.error("Error while fetching email recipients: " + e.getMessage(), e);
            } finally {
                try { conn.close(); } catch (SQLException ignore) {}
            }
        }
    }
    return emailList;
}
    public static boolean isSendNotification(String status, PropertiesLoader pl, String cwi, String table, 
                                       String recipient, String filter_lastDate, String filter_currDate, 
                                       String partitions) {
    Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_idb_name, 
                                    pl.db_username, pl.db_password);
    if (conn != null) {
        try {
            logger.info("Start running query for checking if eligible to send " + table);
            String query = "SELECT DISTINCT d.RECIPIENTS " +
                        "FROM ACTONE.v_items_parent_child_relations aa " +
                        "JOIN ACTONE.alerts a ON aa.item_id = a.alert_id " +
                        "JOIN ACTONE.acm_md_alert_statuses b ON a.status_internal_id = b.status_internal_id " +
                        "JOIN ACTONE.alerts c ON aa.child_id = c.alert_id " +
                        "JOIN IDB_DATA_USER.ff_all_transactions trx ON c.p50 = trx.ACTIMIZE_TRANSACTION_IDENTITY " +
                        "JOIN IDB_DATA_USER.IMPL_" + table + "_ALERT_NOTIFICATION d ON d.TRX_DATE::date = trx.TRX_NORMALIZED_DATETIME::date " +
                        "WHERE b.status_name = 'Assigned' " +
                        "AND d.PARTY_ID = trx.PARTY_KEY " +
                        "AND d.SEND_STATUS::int = 1 " +
                        "AND d.RECIPIENTS = '" + recipient + "' " +
                        "AND EXTRACT(DAY FROM trx.TRX_NORMALIZED_DATETIME) IN (" + partitions + ")";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            if (rs.next()) {
                logger.info("Finish running query for checking if eligible to send " + table + ". Notification will not be send");
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error checking notification eligibility: " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.error("Failed to close DB connection: " + e.getMessage());
            }
        }
    }
    logger.info("Finish running query for checking if eligible to send " + table + ". Notification will be send");
    return true;
}
    public static Map<String, String> getEmailTemplate(String type, PropertiesLoader pl) {
        Map<String, String> template = new HashMap<>();
        String query = "SELECT email_subject, email_body " +
                    "FROM idb_data_user.impl_email_template " + // UBAH SCHEMA DI SINI
                    "WHERE type = ? LIMIT 1";

        try (Connection conn = getDBConnection(pl.db_host, pl.db_port, pl.db_idb_name, // GUNAKAN IDB CONNECTION
                                            pl.db_username, pl.db_password);
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, type);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                template.put("subject", rs.getString("email_subject"));
                template.put("body", rs.getString("email_body"));
            }

        } catch (SQLException e) {
            logger.error("Error fetching email template: " + e.getMessage(), e);
        }
        return template;
    }    
    public static void updateAlertStatus(
        Connection conn, 
        List<MessageContent> alerts, 
        boolean success,
        ArrayList<String> recipients
) {
    if (alerts.isEmpty()) return;

    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    String recipientStr = String.join(";", recipients);

    // Build IN clause
    StringBuilder inClause = new StringBuilder(" IN (");
    for (int i = 0; i < alerts.size(); i++) {
        inClause.append("'").append(alerts.get(i).getTrx_key()).append("'");
        if (i < alerts.size() - 1) inClause.append(",");
    }
    inClause.append(")");

    try (Statement stmt = conn.createStatement()) {

        // Log status asli sebelum email send (misal untuk debug)
        String checkQuery = "SELECT trx_key, SEND_STATUS FROM IDB_DATA_USER.IMPL_EMAIL_ALERT_NOTIFICATION " +
                            "WHERE trx_key " + inClause.toString();
        Map<String, String> originalStatus = new HashMap<>();
        try (ResultSet rs = stmt.executeQuery(checkQuery)) {
            while (rs.next()) {
                originalStatus.put(rs.getString("trx_key"), rs.getString("SEND_STATUS"));
            }
        }
        originalStatus.forEach((key, status) ->
                logger.info("Original status before email send - trx_key: " + key + ", SEND_STATUS: " + status));

        //  Log status sebelum update
        Map<String, String> beforeUpdateStatus = new HashMap<>();
        try (ResultSet rs = stmt.executeQuery(checkQuery)) {
            while (rs.next()) {
                beforeUpdateStatus.put(rs.getString("trx_key"), rs.getString("SEND_STATUS"));
            }
        }
        beforeUpdateStatus.forEach((key, status) ->
                logger.info("Before update - trx_key: " + key + ", SEND_STATUS: " + status));

        //  Build update query
        String updateQuery;
        if (success) {
            updateQuery = "UPDATE IDB_DATA_USER.IMPL_EMAIL_ALERT_NOTIFICATION " +
                          "SET SEND_STATUS = '1', " +
                          "send_count = CAST(CAST(send_count AS integer) + 1 AS text), " +
                          "time_stamp = '" + timestamp + "', " +
                          "recipients = '" + recipientStr + "' " +
                          "WHERE SEND_STATUS = '0' AND trx_key " + inClause.toString();
        } else {
            updateQuery = "UPDATE IDB_DATA_USER.IMPL_EMAIL_ALERT_NOTIFICATION " +
                          "SET send_count = CAST(CAST(send_count AS integer) + 1 AS text), " +
                          "time_stamp = '" + timestamp + "', " +
                          "recipients = '" + recipientStr + "' " +
                          "WHERE SEND_STATUS = '0' AND trx_key " + inClause.toString();
        }

        int updated = stmt.executeUpdate(updateQuery);
        logger.info("Executed update statement for " + alerts.size() + " alerts, changed rows: " + updated);

        // Log status sesudah update
        try (ResultSet rs = stmt.executeQuery(checkQuery)) {
            while (rs.next()) {
                logger.info("After update - trx_key: " + rs.getString("trx_key") +
                            ", SEND_STATUS: " + rs.getString("SEND_STATUS"));
            }
        }

    } catch (SQLException e) {
        logger.error("Error updating alert status: " + e.getMessage(), e);
    }
}

    public static void main(String[] args) {
    if (args.length < 1) {
        logger.error("Please provide argument");
        System.exit(1);
    }
    
    logger.info(" Starting AlertNotifApp with config file: " + args[0]);


        Logging logging = new Logging();
        PropertiesLoader pl = new PropertiesLoader(args[0]);
        logging.configLog(pl, logger, loggerContext);    logger.info("Loaded properties - DB: " + pl.db_host + ":" + pl.db_port + "/" + pl.db_name);

    String partitions = getDBPartitionKey(pl);
    logger.info("Table Partition Found = " + partitions);

    if (partitions.isEmpty()) {
        logger.error("No partitions found, exiting...");
        return;
    }

    Connection idb_conn = getDBConnection(pl.db_host, pl.db_port, pl.db_idb_name, pl.db_username, pl.db_password);
    Connection rcm_conn = getDBConnection(pl.db_host, pl.db_port, pl.db_name, pl.db_username, pl.db_password);
    if (idb_conn == null || rcm_conn == null) {
        logger.error("Failed to establish database connection");
        System.exit(1);
    }

    logger.info("Successfully connected to database: " + pl.db_name);


    try {
        // Ambil semua data dalam 1 query
        String query = "SELECT aa.item_id AS CWI, alstat.status_name AS STATUS_ID, " +
                       "trx.*, c.* " +
                       "FROM ACTONE.v_items_parent_child_relations aa " +
                       "JOIN ACTONE.alerts ab ON aa.child_id = ab.alert_id " +
                       "JOIN IDB_DATA_USER.ff_all_transactions trx " +
                       "     ON ab.p50 = trx.ACTIMIZE_TRANSACTION_IDENTITY " +
                       "     AND EXTRACT(DAY FROM trx.TRX_NORMALIZED_DATETIME) IN (" + partitions + ") " +
                       "JOIN IDB_DATA_USER.IMPL_EMAIL_ALERT_NOTIFICATION c " +
                       "     ON c.TRX_DATE::date = trx.TRX_NORMALIZED_DATETIME::date " +
                       "     AND c.PARTY_ID = trx.PARTY_KEY::text " +
                       "JOIN ACTONE.alerts ac ON aa.item_id = ac.alert_id " +
                       "JOIN ACTONE.acm_md_alert_statuses alstat " +
                       "     ON alstat.status_internal_id = ac.status_internal_id " +
                       "WHERE c.SEND_STATUS::int = 0 AND c.SEND_COUNT::int < 4 " +
                       "ORDER BY aa.item_id";

        logger.info("MAIN QUERY: " + query);     
        Statement stmt = idb_conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        logger.info("Main query executed successfully");

        // Group per CWI
        Map<String, List<MessageContent>> alertsByCwi = new HashMap<>();
        Map<String, String> statusByCwi = new HashMap<>();

        while (rs.next()) {
            String cwi = rs.getString("CWI");

            MessageContent msg = new MessageContent();
            msg.setTrx_key(rs.getString("TRX_KEY"));
            msg.setAlert_id(cwi);
            msg.setParty_id(rs.getString("PARTY_ID"));
            msg.setCust_name(rs.getString("CUST_NAME"));
            msg.setTrx_type(rs.getString("TRX_TYPE"));
            String amt = rs.getString("TRX_AMOUNT");
            if (amt == null || amt.isEmpty()) amt = "0";
            else amt = String.valueOf((long) Double.parseDouble(amt));
            msg.setTrx_amt(amt);
            msg.setResponse(rs.getString("RESPONSE"));      
            msg.setScore(rs.getString("SCORE"));
            msg.setChannel_type(rs.getString("CHANNEL_TYPE"));
            msg.setTrx_date(rs.getString("TRX_DATE"));
            msg.setRecipients(rs.getString("RECIPIENTS"));

            alertsByCwi.computeIfAbsent(cwi, k -> new ArrayList<>()).add(msg);
            statusByCwi.put(cwi, rs.getString("STATUS_ID"));
        }
        
        logger.info("Grouped into " + alertsByCwi.size() + " CWI groups");

        MessageBuilder mb = new MessageBuilder(pl, rcm_conn);

        // Loop per CWI (mirip reference)
        for (Map.Entry<String, List<MessageContent>> entry : alertsByCwi.entrySet()) {
            String cwi = entry.getKey();
            List<MessageContent> listMessage = entry.getValue();
            String status = statusByCwi.get(cwi);
            logger.info("Processing CWI: " + cwi + " with " + listMessage.size() + " alerts, Status: " + status);

        ArrayList<String> recipients = new ArrayList<>();
            for (MessageContent msg : listMessage) {
                String rec = msg.getRecipients(); // 
                if (rec != null && !rec.isEmpty()) {
                    // split kalau multiple recipients dipisah ;
                    for (String r : rec.split(";")) {
                        if (!r.trim().isEmpty()) recipients.add(r.trim());
                    }
                }
            }
            logger.info("Recipients for status '" + status + "': " + recipients);


            if (recipients.isEmpty()) continue;

            String firstRecipient = recipients.isEmpty() ? "" : recipients.get(0);
            if (!isSendNotification(status, pl, cwi, "EMAIL", firstRecipient, 
                                  "", "", partitions)) {  // filter dates kosong karena tidak dipakai
                logger.info("Notification already sent to " + firstRecipient + " for CWI: " + cwi);
                continue;
            }
            logger.info("Eligible to send notification for CWI: " + cwi);

            // Build email
            EmailObject emailObj = new EmailObject();
            emailObj.setSmtp_host(pl.smtp_host);
            emailObj.setSmtp_port(pl.smtp_port);
            emailObj.setEmail_user(pl.email_username);
            emailObj.setEmail_password(pl.email_password);
            emailObj.setEmail_subject("Alert Notification - " + cwi);
            emailObj.setList_message(new ArrayList<>(listMessage));
            emailObj.setEmail_message(mb.emailMessageBuilder(new ArrayList<>(listMessage), firstRecipient));
            emailObj.setList_recipients(recipients);

            EmailSenderApp emailSender = new EmailSenderApp();
            boolean sendResult = emailSender.sendEmail(emailObj);

            logger.info("Email send result for CWI " + cwi + ": " + (sendResult ? "SUCCESS" : "FAILED"));
            // Update status semua trx_key untuk CWI ini
            updateAlertStatus(rcm_conn, listMessage, sendResult, recipients);
        }

    } catch (Exception e) {
        logger.error("Unexpected error: " + e.getMessage(), e);
    } finally {
        try { if (rcm_conn != null) rcm_conn.close(); } catch (SQLException ignored) {}
        try { if (idb_conn != null) idb_conn.close(); } catch (SQLException ignored) {}
    }
}
}
