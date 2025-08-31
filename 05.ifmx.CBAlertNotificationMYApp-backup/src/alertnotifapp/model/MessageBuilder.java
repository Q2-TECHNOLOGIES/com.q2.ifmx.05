package alertnotifapp.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;


import alertnotifapp.PropertiesLoader;
public class MessageBuilder {
    private PropertiesLoader pl;
    private Connection conn;
    
    public MessageBuilder(PropertiesLoader pl, Connection conn) {
        this.pl = pl;
        this.conn = conn;
    }
    public String emailMessageBuilder(ArrayList<MessageContent> alerts, String recipient) {
    // Try to get template from database first
    String template = getTemplateFromDB();
    if (template == null) {
        // Fallback to default template
        template = "<html><body>"
                 + "<h2>Alert Notification</h2>"
                 + "<p>Dear [Recipient],</p>"
                 + "<p>There is a new alert with the following details:</p>"
                 + "<table border='1' style='border-collapse: collapse;'>"
                 + "<tr><th>Date</th><th>Alert ID</th><th>Customer</th><th>Type</th><th>Amount</th><th>Response</th><th>Score</th><th>Channel</th></tr>"
                 + "<!-- ALERTS_PLACEHOLDER -->"
                 + "</table></body></html>";
    }
    
    // Replace placeholders with actual values
    return populateTemplate(template, alerts, recipient);
}

private String getTemplateFromDB() {
    try {
        // pastikan conn adalah instance variable atau passed in
        String query = "SELECT email_body FROM idb_data_user.impl_email_template WHERE type = 'CWI_ALERT'";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            return rs.getString("email_body");
        }
    } catch (SQLException e) {
        System.err.println("Error getting template from DB: " + e.getMessage());
    }
    return null;
}

private String populateTemplate(String template, ArrayList<MessageContent> alerts, String recipient) {
    if (alerts.isEmpty()) return template;

    String populated = template;

    // Ganti placeholder [Recipient]
    populated = populated.replace("[Recipient]", recipient != null ? recipient : "");

    // Ambil alert pertama saja untuk single-email format
    MessageContent alert = alerts.get(0);

    populated = populated.replace("[Transaction_date]", alert.getTrx_date() != null ? alert.getTrx_date() : "")
                         .replace("[Alert_ID]", alert.getAlert_id() != null ? alert.getAlert_id() : "")
                         .replace("[Party_id]", alert.getParty_id() != null ? alert.getParty_id() : "")
                         .replace("[Customer_name]", alert.getCust_name() != null ? alert.getCust_name() : "")
                         .replace("[Transaction_type]", alert.getTrx_type() != null ? alert.getTrx_type() : "")
                         .replace("[Transaction_amount]", alert.getTrx_amt() != null ? alert.getTrx_amt() : "")
                         .replace("[Response]", alert.getResponse() != null ? alert.getResponse() : "")
                         .replace("[Score]", alert.getScore() != null ? alert.getScore() : "")
                         .replace("[Channel_type]", alert.getChannel_type() != null ? alert.getChannel_type() : "");

    return populated;
}



}
