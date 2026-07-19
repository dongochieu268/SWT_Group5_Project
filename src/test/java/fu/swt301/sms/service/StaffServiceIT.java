package fu.swt301.sms.service;

import fu.swt301.sms.dao.ConnectionProvider;
import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StaffServiceIT {
    private static final String URL =
            "jdbc:h2:mem:staff-service;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

    private ConnectionProvider connectionProvider;

    @Before
    public void setUpDatabase() throws Exception {
        Class.forName("org.h2.Driver");
        connectionProvider = () -> DriverManager.getConnection(URL, "sa", "");
        try (Connection conn = connectionProvider.getConnection()) {
            execute(conn, "CREATE TABLE Role (Role_ID INT PRIMARY KEY, Role_Name VARCHAR(50) NOT NULL)");
            execute(conn,
                    "CREATE TABLE Staff ("
                            + "StaffID INT IDENTITY(1,1) PRIMARY KEY,"
                            + "EmployeeCode VARCHAR(20) NOT NULL,"
                            + "FullName VARCHAR(100) NOT NULL,"
                            + "Gender BIT NOT NULL,"
                            + "DateOfBirth DATE NOT NULL,"
                            + "PhoneNumber VARCHAR(10) NOT NULL,"
                            + "Email VARCHAR(100) NOT NULL,"
                            + "PasswordHash VARCHAR(255) NOT NULL,"
                            + "Department VARCHAR(100) NOT NULL,"
                            + "Position VARCHAR(100) NOT NULL,"
                            + "Salary DECIMAL(18,2) NOT NULL,"
                            + "HireDate DATE NOT NULL,"
                            + "Role_ID INT NOT NULL,"
                            + "IsActive BIT NOT NULL,"
                            + "Deleted BIT NOT NULL DEFAULT 0,"
                            + "FOREIGN KEY (Role_ID) REFERENCES Role(Role_ID))");
            execute(conn, "INSERT INTO Role (Role_ID, Role_Name) VALUES (2, 'Staff')");
        }
    }

    @After
    public void cleanDatabase() throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement("DROP ALL OBJECTS")) {
            ps.execute();
        }
    }

    @Test
    public void getStaffByIdReturnsStaffThroughDaoAndDatabase() throws Exception {
        int staffId = insertStaff();
        StaffService staffService = new StaffService(
                new StaffDAO(connectionProvider), new RoleDAO(connectionProvider));

        Staff result = staffService.getStaffById(staffId);

        assertNotNull(result);
        assertEquals(staffId, result.getStaffID());
        assertEquals("SVC900", result.getEmployeeCode());
        assertEquals("Service Integration Staff", result.getFullName());
        assertEquals("Quality", result.getDepartment());
        assertNotNull(result.getRole());
        assertEquals(2, result.getRole().getRoleID());
        assertEquals("Staff", result.getRole().getRoleName());
    }

    private int insertStaff() throws Exception {
        String sql = "INSERT INTO Staff (EmployeeCode, FullName, Gender, DateOfBirth, PhoneNumber, "
                + "Email, PasswordHash, Department, Position, Salary, HireDate, Role_ID, IsActive, Deleted) "
                + "VALUES ('SVC900', 'Service Integration Staff', 1, CAST('1995-01-01' AS DATE), "
                + "'0987654321', 'service-it@example.com', 'unused-hash', 'Quality', 'Tester', "
                + "1000.00, CAST('2026-07-18' AS DATE), 2, 1, 0)";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void execute(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }
}
