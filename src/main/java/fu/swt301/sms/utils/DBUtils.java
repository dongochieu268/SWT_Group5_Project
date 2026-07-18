package fu.swt301.sms.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtils {
    private static final String DB_URL_ENV = "SMS_DB_URL";
    private static final String DB_USER_ENV = "SMS_DB_USER";
    private static final String DB_PASSWORD_ENV = "SMS_DB_PASSWORD";
    private static final String DEFAULT_DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=StaffManagement;encrypt=true;trustServerCertificate=true";
    private static final String DEFAULT_DB_USER = "sa";
    private static final String DEFAULT_DB_PASSWORD = "123";

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String url = getEnvironmentVariableOrDefault(DB_URL_ENV, DEFAULT_DB_URL);
        String username = getEnvironmentVariableOrDefault(DB_USER_ENV, DEFAULT_DB_USER);
        String password = getEnvironmentVariableOrDefault(DB_PASSWORD_ENV, DEFAULT_DB_PASSWORD);
        return DriverManager.getConnection(url, username, password);
    }

    private static String getEnvironmentVariableOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
