package fu.swt301.sms.config;

import fu.swt301.sms.utils.DBUtils;
import fu.swt301.sms.utils.PasswordUtils;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
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
            ensureStaffListColumns(conn);
            ensureStaffLockoutColumns(conn);

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
                               "RoleID INT PRIMARY KEY, " +
                               "RoleName NVARCHAR(50) NOT NULL UNIQUE" +
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
                               "EmployeeCode VARCHAR(50), " +
                               "FullName NVARCHAR(100) NOT NULL, " +
                               "Department NVARCHAR(100), " +
                               "Gender BIT NOT NULL, " +
                               "PhoneNumber VARCHAR(20), " +
                               "Email VARCHAR(100) NOT NULL UNIQUE, " +
                               "PasswordHash VARCHAR(255) NOT NULL, " +
                               "RoleID INT NOT NULL, " +
                               "IsActive BIT NOT NULL, " +
                               "Deleted BIT NOT NULL DEFAULT 0, " +
                               "FailedLoginAttempts INT NOT NULL DEFAULT 0, " +
                               "LockUntil DATETIME2 NULL, " +
                               "CONSTRAINT FK_Staff_Role FOREIGN KEY (RoleID) REFERENCES Role(RoleID)" +
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
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Role (RoleID, RoleName) VALUES (?, ?)")) {
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
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Staff (FullName, Gender, PhoneNumber, Email, PasswordHash, RoleID, IsActive, Deleted) VALUES (?, ?, ?, ?, ?, ?, ?, 0)")) {
            ps.setString(1, "Admin User");
            ps.setBoolean(2, true); // true for Male
            ps.setString(3, "0123456789");
            ps.setString(4, "admin@example.com");
            ps.setString(5, PasswordUtils.hashPassword("admin123"));
            ps.setInt(6, 1); // RoleID for Admin
            ps.setBoolean(7, true); // IsActive
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

    private void ensureStaffListColumns(Connection conn) throws SQLException {
        if (!columnExists(conn, "Staff", "EmployeeCode")) {
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE Staff ADD EmployeeCode VARCHAR(50) NULL")) {
                ps.execute();
                System.out.println("Column 'EmployeeCode' added to Staff.");
            }
        }

        if (!columnExists(conn, "Staff", "Department")) {
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE Staff ADD Department NVARCHAR(100) NULL")) {
                ps.execute();
                System.out.println("Column 'Department' added to Staff.");
            }
        }
    }

    /**
     * Adds the columns FR-03 (account lockout) relies on, for databases created
     * before this feature existed. Safe to run on every startup.
     * @param conn The active database connection.
     * @throws SQLException if a database access error occurs.
     */
    private void ensureStaffLockoutColumns(Connection conn) throws SQLException {
        if (!columnExists(conn, "Staff", "FailedLoginAttempts")) {
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE Staff ADD FailedLoginAttempts INT NOT NULL DEFAULT 0")) {
                ps.execute();
                System.out.println("Column 'FailedLoginAttempts' added to Staff.");
            }
        }

        if (!columnExists(conn, "Staff", "LockUntil")) {
            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE Staff ADD LockUntil DATETIME2 NULL")) {
                ps.execute();
                System.out.println("Column 'LockUntil' added to Staff.");
            }
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
