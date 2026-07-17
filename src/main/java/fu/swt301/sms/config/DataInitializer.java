package fu.swt301.sms.config;

import fu.swt301.sms.utils.DBUtils;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
                               "FullName NVARCHAR(100) NOT NULL, " +
                               "Gender BIT NOT NULL, " +
                               "PhoneNumber VARCHAR(20), " +
                               "Email VARCHAR(100) NOT NULL UNIQUE, " +
                               "Password VARCHAR(255) NOT NULL, " +
                               "Role_ID INT NOT NULL, " +
                               "IsActive BIT NOT NULL, " +
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
        // IMPORTANT: The password here is plain text. In a real-world application, this should be hashed.
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Staff (FullName, Gender, PhoneNumber, Email, Password, Role_ID, IsActive) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "Admin User");
            ps.setBoolean(2, true); // true for Male
            ps.setString(3, "0123456789");
            ps.setString(4, "admin@example.com");
            ps.setString(5, "admin123"); // WARNING: Plain text password
            ps.setInt(6, 1); // Role_ID for Admin
            ps.setBoolean(7, true); // IsActive
            ps.executeUpdate();
            System.out.println("Default admin user inserted.");
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
