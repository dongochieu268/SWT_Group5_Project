package fu.swt301.sms.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtils {
    private static final String DB_URL_ENV = "SMS_DB_URL";
    private static final String DB_USER_ENV = "SMS_DB_USER";
    private static final String DB_PASSWORD_ENV = "SMS_DB_PASSWORD";

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = getRequiredEnvironmentVariable(DB_URL_ENV);
        String username = getRequiredEnvironmentVariable(DB_USER_ENV);
        String password = getRequiredEnvironmentVariable(DB_PASSWORD_ENV);
        return DriverManager.getConnection(url, username, password);
    }

    private static String getRequiredEnvironmentVariable(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException("Missing required environment variable: " + name);
        }
        return value;
    }
}
