package common;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
public class DatabaseLogger {
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;

    public DatabaseLogger(PropertiesLoader pl) {
        this.dbUrl = pl.db_url;
        this.dbUsername = pl.db_username;
        this.dbPassword = pl.db_password;
    }

    public void logStatusCode(String statusCode) {
        String sql = "INSERT INTO IMPL_api_responses (status_code) VALUES (?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, statusCode);
            pstmt.executeUpdate();
            
        } catch (Exception e) {
            System.err.println("Error logging status code to database: " + e.getMessage());
            // You might want to log this error to your file logger as well
        }
    }
}
