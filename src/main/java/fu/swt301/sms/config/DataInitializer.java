package fu.swt301.sms.config;

import fu.swt301.sms.utils.DBUtils;
import fu.swt301.sms.utils.PasswordUtils;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This listener class is automatically instantiated and invoked by the web container when the application starts up.
 * Its primary purpose is to initialize the database by:
 * 1. Creating the necessary tables ('Role', 'Staff') if they do not already exist.
 * 2. Seeding the tables with default data (e.g., user roles and a default admin account) if they are empty.
 * This makes the application self-contained and easier to deploy.
 */
@WebListener
public class DataInitializer implements ServletContextListener {

    /**
     * This method is called by the container when the web application is first started.
     * It orchestrates the database initialization process.
     * @param sce The event object containing the ServletContext.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try (Connection conn = DBUtils.getConnection()) {
            // Step 1: Ensure database tables are created before proceeding.
            System.out.println("Checking database schema...");
            createRoleTableIfNotExists(conn);
            createStaffTableIfNotExists(conn);
            ensureStaffAuthColumns(conn);
            ensureStaffProfileColumns(conn);

            // Step 2: Check if the 'Role' table is empty. If it is, we assume the database is new and needs seeding.
            boolean dataExists = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Role");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    dataExists = true;
                }
            }

            // Step 3: If no data exists, insert the default roles and a default admin user.
            if (!dataExists) {
                System.out.println("No data found. Initializing default data...");
                insertDefaultData(conn);
            } else {
                System.out.println("Data already exists. Skipping initialization.");
            }

        } catch (SQLException | ClassNotFoundException e) {
            // If any database error occurs during initialization, log it and throw a RuntimeException
            // to halt the application's startup, as it cannot function without a proper database setup.
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database.", e);
        }
    }

    /**
     * Checks if the 'Role' table exists in the database. If not, it creates the table.
     * @param conn The active database connection.
     * @throws SQLException if a database access error occurs.
     */
    private void createRoleTableIfNotExists(Connection conn) throws SQLException {
        String tableName = "Role";
        String checkTableSQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
        boolean tableExists = false;

        try (PreparedStatement ps = conn.prepareStatement(checkTableSQL)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tableExists = true;
                }
            }
        }

        if (!tableExists) {
            System.out.println("Table 'Role' not found. Creating table...");
            String createSQL = "CREATE TABLE Role (" +
                               "Role_ID INT PRIMARY KEY, " +
                               "Role_Name NVARCHAR(50) NOT NULL UNIQUE" +
                               ")";
            try (PreparedStatement ps = conn.prepareStatement(createSQL)) {
                ps.execute();
                System.out.println("Table 'Role' created.");
            }
        }
    }

    /**
     * Checks if the 'Staff' table exists in the database. If not, it creates the table
     * with a foreign key constraint pointing to the 'Role' table.
     * @param conn The active database connection.
     * @throws SQLException if a database access error occurs.
     */
    private void createStaffTableIfNotExists(Connection conn) throws SQLException {
        String tableName = "Staff";
        String checkTableSQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
        boolean tableExists = false;

        try (PreparedStatement ps = conn.prepareStatement(checkTableSQL)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tableExists = true;
                }
            }
        }

        if (!tableExists) {
            System.out.println("Table 'Staff' not found. Creating table...");
            String createSQL = "CREATE TABLE Staff (" +
                               "StaffID INT PRIMARY KEY IDENTITY(1,1), " +
                               "EmployeeCode VARCHAR(20) NOT NULL, " +
                               "FullName NVARCHAR(100) NOT NULL, " +
                               "Gender BIT NOT NULL, " +
                               "DateOfBirth DATE NOT NULL, " +
                               "PhoneNumber VARCHAR(10) NOT NULL, " +
                               "Email VARCHAR(100) NOT NULL, " +
                               "PasswordHash VARCHAR(255) NOT NULL, " +
                               "Department NVARCHAR(100) NOT NULL, " +
                               "Position NVARCHAR(100) NOT NULL, " +
                               "Salary DECIMAL(18,2) NOT NULL, " +
                               "HireDate DATE NOT NULL, " +
                               "Role_ID INT NOT NULL, " +
                               "IsActive BIT NOT NULL, " +
                               "Deleted BIT NOT NULL DEFAULT 0, " +
                               "CONSTRAINT CK_Staff_Salary CHECK (Salary >= 0), " +
                               "CONSTRAINT FK_Staff_Role FOREIGN KEY (Role_ID) REFERENCES Role(Role_ID)" +
                               ")";
            try (PreparedStatement ps = conn.prepareStatement(createSQL)) {
                ps.execute();
                System.out.println("Table 'Staff' created.");
            }
        }
    }

    /**
     * Inserts a predefined set of data into the 'Role' and 'Staff' tables.
     * This includes 'Admin' and 'Staff' roles, and a default administrator account.
     * @param conn The active database connection.
     * @throws SQLException if a database access error occurs.
     */
    private void insertDefaultData(Connection conn) throws SQLException {
        // Insert default roles using a batch operation for efficiency.
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Role (Role_ID, Role_Name) VALUES (?, ?)")) {
            ps.setInt(1, 1);
            ps.setString(2, "Admin");
            ps.addBatch();

            ps.setInt(1, 2);
            ps.setString(2, "Staff");
            ps.addBatch();

            ps.executeBatch();
            System.out.println("Default roles inserted.");
        }

        // Insert a default administrator user for initial login.
        String sql = "INSERT INTO Staff (EmployeeCode, FullName, Gender, DateOfBirth, PhoneNumber, "
                + "Email, PasswordHash, Department, Position, Salary, HireDate, Role_ID, IsActive, Deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "ADM001");
            ps.setString(2, "Admin User");
            ps.setBoolean(3, true);
            ps.setDate(4, Date.valueOf("1990-01-01"));
            ps.setString(5, "0123456789");
            ps.setString(6, "admin@example.com");
            ps.setString(7, PasswordUtils.hashPassword("admin123"));
            ps.setString(8, "Administration");
            ps.setString(9, "Administrator");
            ps.setBigDecimal(10, java.math.BigDecimal.ZERO);
            ps.setDate(11, Date.valueOf("2020-01-01"));
            ps.setInt(12, 1);
            ps.setBoolean(13, true);
            ps.executeUpdate();
            System.out.println("Default admin user inserted.");
        }
    }

    private void ensureStaffAuthColumns(Connection conn) throws SQLException {
        if (!columnExists(conn, "Staff", "PasswordHash")) {
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE Staff ADD PasswordHash VARCHAR(255) NULL")) {
                ps.execute();
                System.out.println("Column 'PasswordHash' added to Staff.");
            }
        }

        if (!columnExists(conn, "Staff", "Deleted")) {
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE Staff ADD Deleted BIT NOT NULL DEFAULT 0")) {
                ps.execute();
                System.out.println("Column 'Deleted' added to Staff.");
            }
        }

        if (columnExists(conn, "Staff", "Password")) {
            List<Integer> staffIds = new ArrayList<>();
            List<String> passwordHashes = new ArrayList<>();
            String selectSql = "SELECT StaffID, Password, PasswordHash FROM Staff";
            try (PreparedStatement select = conn.prepareStatement(selectSql);
                 ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    String currentHash = rs.getString("PasswordHash");
                    String legacyPassword = rs.getString("Password");
                    if (!PasswordUtils.isBCryptHash(currentHash)) {
                        String passwordToMigrate = currentHash != null && !currentHash.isEmpty()
                                ? currentHash
                                : legacyPassword;
                        if (passwordToMigrate == null || passwordToMigrate.isEmpty()) {
                            throw new SQLException("Cannot migrate empty password for staff " + rs.getInt("StaffID"));
                        }
                        staffIds.add(rs.getInt("StaffID"));
                        passwordHashes.add(PasswordUtils.hashPassword(passwordToMigrate));
                    }
                }
            }

            try (PreparedStatement update = conn.prepareStatement("UPDATE Staff SET PasswordHash = ? WHERE StaffID = ?")) {
                for (int i = 0; i < staffIds.size(); i++) {
                    update.setString(1, passwordHashes.get(i));
                    update.setInt(2, staffIds.get(i));
                    update.addBatch();
                }
                update.executeBatch();
            }

            try (PreparedStatement alter = conn.prepareStatement("ALTER TABLE Staff ALTER COLUMN PasswordHash VARCHAR(255) NOT NULL")) {
                alter.execute();
            }
            try (PreparedStatement drop = conn.prepareStatement("ALTER TABLE Staff DROP COLUMN Password")) {
                drop.execute();
                System.out.println("Legacy password column removed from Staff.");
            }
        }
    }

    private void ensureStaffProfileColumns(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "EmployeeCode", "VARCHAR(20) NULL");
        addColumnIfMissing(conn, "DateOfBirth", "DATE NULL");
        addColumnIfMissing(conn, "Department", "NVARCHAR(100) NULL");
        addColumnIfMissing(conn, "Position", "NVARCHAR(100) NULL");
        addColumnIfMissing(conn, "Salary", "DECIMAL(18,2) NULL");
        addColumnIfMissing(conn, "HireDate", "DATE NULL");

        executeSchemaUpdate(conn,
                "UPDATE Staff SET EmployeeCode = CONCAT('LEGACY', StaffID) WHERE EmployeeCode IS NULL");
        executeSchemaUpdate(conn,
                "UPDATE Staff SET DateOfBirth = '1900-01-01' WHERE DateOfBirth IS NULL");
        executeSchemaUpdate(conn,
                "UPDATE Staff SET Department = 'Legacy' WHERE Department IS NULL");
        executeSchemaUpdate(conn,
                "UPDATE Staff SET Position = 'Legacy' WHERE Position IS NULL");
        executeSchemaUpdate(conn,
                "UPDATE Staff SET Salary = 0 WHERE Salary IS NULL");
        executeSchemaUpdate(conn,
                "UPDATE Staff SET HireDate = CAST(GETDATE() AS DATE) WHERE HireDate IS NULL");

        alterColumnIfNullable(conn, "EmployeeCode", "VARCHAR(20) NOT NULL");
        alterColumnIfNullable(conn, "DateOfBirth", "DATE NOT NULL");
        alterColumnIfNullable(conn, "Department", "NVARCHAR(100) NOT NULL");
        alterColumnIfNullable(conn, "Position", "NVARCHAR(100) NOT NULL");
        alterColumnIfNullable(conn, "Salary", "DECIMAL(18,2) NOT NULL");
        alterColumnIfNullable(conn, "HireDate", "DATE NOT NULL");

        dropLegacyEmailUniqueConstraint(conn);
        ensureFilteredUniqueIndex(conn, "UX_Staff_EmployeeCode", "EmployeeCode");
        ensureFilteredUniqueIndex(conn, "UX_Staff_Email", "Email");
    }

    private void addColumnIfMissing(Connection conn, String columnName, String definition)
            throws SQLException {
        if (!columnExists(conn, "Staff", columnName)) {
            executeSchemaUpdate(conn, "ALTER TABLE Staff ADD " + columnName + " " + definition);
        }
    }

    private void alterColumnIfNullable(Connection conn, String columnName, String definition)
            throws SQLException {
        if (columnAllowsNull(conn, "Staff", columnName)) {
            executeSchemaUpdate(conn, "ALTER TABLE Staff ALTER COLUMN "
                    + columnName + " " + definition);
        }
    }

    private boolean columnAllowsNull(Connection conn, String tableName, String columnName)
            throws SQLException {
        String sql = "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
            }
        }
    }

    private void dropLegacyEmailUniqueConstraint(Connection conn) throws SQLException {
        String sql = "DECLARE @constraintName NVARCHAR(128); "
                + "SELECT TOP 1 @constraintName = kc.name "
                + "FROM sys.key_constraints kc "
                + "JOIN sys.index_columns ic ON kc.parent_object_id = ic.object_id "
                + "AND kc.unique_index_id = ic.index_id "
                + "JOIN sys.columns c ON ic.object_id = c.object_id "
                + "AND ic.column_id = c.column_id "
                + "WHERE kc.parent_object_id = OBJECT_ID('Staff') "
                + "AND kc.type = 'UQ' AND c.name = 'Email'; "
                + "IF @constraintName IS NOT NULL "
                + "EXEC('ALTER TABLE Staff DROP CONSTRAINT [' + @constraintName + ']');";
        executeSchemaUpdate(conn, sql);
    }

    private void ensureFilteredUniqueIndex(Connection conn, String indexName, String columnName)
            throws SQLException {
        String sql = "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = '" + indexName
                + "' AND object_id = OBJECT_ID('Staff')) "
                + "CREATE UNIQUE INDEX " + indexName + " ON Staff(" + columnName
                + ") WHERE Deleted = 0";
        executeSchemaUpdate(conn, sql);
    }

    private void executeSchemaUpdate(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * This method is called by the container when the web application is about to be shut down.
     * No cleanup action is needed in this case.
     * @param sce The event object containing the ServletContext.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // No action needed on shutdown.
    }
}
