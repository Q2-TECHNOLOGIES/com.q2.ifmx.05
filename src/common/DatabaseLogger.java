package common;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
public class DatabaseLogger {
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final boolean dbConfigValid;

    public DatabaseLogger(PropertiesLoader pl){ 
    
        this.dbConfigValid = isDbConfigValid(pl);    
        if (dbConfigValid) {
            this.dbUrl = "jdbc:postgresql://" + pl.DB_HOST + ":" + pl.DB_PORT + "/" + pl.DB_ACTONE_NAME;
            this.dbUsername = pl.DB_ACTONE_USERNAME;
            this.dbPassword = pl.DB_ACTONE_PASSWORD;
        } else {
            this.dbUrl = null;
            this.dbUsername = null;
            this.dbPassword = null;
            System.out.println("Database logging disabled - incomplete DB configuration");
        }
    }

    private boolean isDbConfigValid(PropertiesLoader pl) {
        return pl.DB_HOST != null && !pl.DB_HOST.isEmpty() &&
               pl.DB_PORT != null && !pl.DB_PORT.isEmpty() &&
               pl.DB_ACTONE_NAME != null && !pl.DB_ACTONE_NAME.isEmpty() &&
               pl.DB_ACTONE_USERNAME != null && !pl.DB_ACTONE_USERNAME.isEmpty() &&
               pl.DB_ACTONE_PASSWORD != null && !pl.DB_ACTONE_PASSWORD.isEmpty();
    }

    public void logStatusCode(String statusCode,PropertiesLoader pl) {
   if (!dbConfigValid || 
            pl.TABLE_CUSTOM_FIELD == null || pl.TABLE_CUSTOM_FIELD.isEmpty() || 
            pl.CUSTOM_FIELD_NAME == null || pl.CUSTOM_FIELD_NAME.isEmpty()) {
            System.out.println("Skipping DB logging - database not configured");
            return;
        }

    String sql = "INSERT INTO " + pl.TABLE_CUSTOM_FIELD + 
                " (" + pl.CUSTOM_FIELD_NAME + ") VALUES (?)";

    try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setString(1, statusCode);
        pstmt.executeUpdate();
        
    } catch (Exception e) {
        System.err.println("Error logging status code to database: " + e.getMessage());
    }
    }
}
