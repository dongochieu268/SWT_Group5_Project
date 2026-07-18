package fu.swt301.sms.dao;

import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;
import fu.swt301.sms.service.StaffValidationException;
import fu.swt301.sms.utils.PasswordUtils;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StaffDAOIT {
    private static final String URL =
            "jdbc:h2:mem:fr7;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

    private ConnectionProvider connectionProvider;

    @Before
    public void setUpDatabase() throws Exception {
        Class.forName("org.h2.Driver");
        connectionProvider = () -> DriverManager.getConnection(URL, "sa", "");
        try (Connection conn = connectionProvider.getConnection()) {
            execute(conn,
                    "CREATE TABLE Role (Role_ID INT PRIMARY KEY, Role_Name VARCHAR(50) NOT NULL)");
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
    public void createStaffPersistsAllFieldsAndBcryptPassword() throws Exception {
        StaffDAO staffDAO = new StaffDAO(connectionProvider);
        StaffService staffService = new StaffService(
                staffDAO, new RoleDAO(connectionProvider));

        staffService.createStaff(validStaff());

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT EmployeeCode, PasswordHash, Salary FROM Staff WHERE Email = ?")) {
            ps.setString(1, "integration@example.com");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("EMP900", rs.getString("EmployeeCode"));
                assertTrue(PasswordUtils.isBCryptHash(rs.getString("PasswordHash")));
                assertTrue(PasswordUtils.verifyPassword(
                        "staff123", rs.getString("PasswordHash")));
                assertEquals(new BigDecimal("1000.00"), rs.getBigDecimal("Salary"));
            }
        }
    }

    @Test
    public void duplicateEmployeeCodeLeavesDatabaseUnchanged() throws Exception {
        StaffDAO staffDAO = new StaffDAO(connectionProvider);
        StaffService staffService = new StaffService(
                staffDAO, new RoleDAO(connectionProvider));
        staffService.createStaff(validStaff());

        Staff duplicate = validStaff();
        duplicate.setEmail("other@example.com");
        try {
            staffService.createStaff(duplicate);
            fail("Expected duplicate employee code to be rejected");
        } catch (StaffValidationException expected) {
            assertEquals(1, countStaff());
        }
    }

    @Test
    public void softDeletedUniqueValuesCanBeReused() throws Exception {
        StaffDAO staffDAO = new StaffDAO(connectionProvider);
        StaffService staffService = new StaffService(
                staffDAO, new RoleDAO(connectionProvider));
        staffService.createStaff(validStaff());

        try (Connection conn = connectionProvider.getConnection()) {
            execute(conn, "UPDATE Staff SET Deleted = 1 WHERE EmployeeCode = 'EMP900'");
        }

        Staff replacement = validStaff();
        replacement.setEmployeeCode("EMP901");
        staffService.createStaff(replacement);

        assertEquals(2, countStaff());
    }

    private int countStaff() throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Staff");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void execute(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private Staff validStaff() {
        Staff staff = new Staff();
        staff.setEmployeeCode("EMP900");
        staff.setFullName("Integration Staff");
        staff.setGender(true);
        staff.setDateOfBirth(LocalDate.of(1995, 1, 1));
        staff.setPhoneNumber("0912345678");
        staff.setEmail("integration@example.com");
        staff.setPassword("staff123");
        staff.setDepartment("Engineering");
        staff.setPosition("Tester");
        staff.setSalary(new BigDecimal("1000.00"));
        staff.setHireDate(LocalDate.of(2026, 7, 18));
        Role role = new Role();
        role.setRoleID(2);
        staff.setRole(role);
        staff.setIsActive(true);
        return staff;
    }
}
