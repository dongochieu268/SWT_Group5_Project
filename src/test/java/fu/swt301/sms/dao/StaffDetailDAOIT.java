package fu.swt301.sms.dao;

import fu.swt301.sms.config.DataInitializer;
import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.DBUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for FR-10: exercises StaffDAO.getStaffById against a real
 * SQL Server database (see DBUtils / SMS_DB_* env vars), covering an existing
 * id, a non-existing id, and a soft-deleted record. Skips automatically when
 * no test database is reachable, so it does not break `mvn verify` on a
 * machine with no database configured.
 */
public class StaffDetailDAOIT {

    private static int testRoleId;
    private static String testRoleName;

    private final StaffDAO staffDAO = new StaffDAO();
    private int insertedStaffId;

    @BeforeClass
    public static void ensureDatabaseAvailable() {
        try {
            new DataInitializer().contextInitialized(null);
            List<Role> roles = new RoleDAO().getAllRoles();
            Role nonAdminRole = null;
            for (Role role : roles) {
                if (!"Admin".equalsIgnoreCase(role.getRoleName())) {
                    nonAdminRole = role;
                    break;
                }
            }
            Role selectedRole = nonAdminRole != null ? nonAdminRole : roles.get(0);
            testRoleId = selectedRole.getRoleID();
            testRoleName = selectedRole.getRoleName();
        } catch (Exception e) {
            System.out.println("Skipping StaffDetailDAOIT: no reachable test database (" + e.getMessage() + ")");
            Assume.assumeNoException(e);
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (insertedStaffId > 0) {
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM Staff WHERE StaffID = ?")) {
                ps.setInt(1, insertedStaffId);
                ps.executeUpdate();
            }
        }
    }

    @Test
    public void getStaffById_returnsStaff_whenActiveAndNotDeleted() throws Exception {
        insertedStaffId = insertStaff(uniqueEmail("active"), false);

        Staff result = staffDAO.getStaffById(insertedStaffId);

        assertNotNull(result);
        assertEquals(insertedStaffId, result.getStaffID());
        assertNotNull(result.getRole());
        assertEquals(testRoleName, result.getRole().getRoleName());
    }

    @Test
    public void getStaffById_returnsNull_whenIdDoesNotExist() {
        Staff result = staffDAO.getStaffById(Integer.MAX_VALUE - 1);

        assertNull(result);
    }

    @Test
    public void getStaffById_returnsNull_whenStaffIsSoftDeleted() throws Exception {
        insertedStaffId = insertStaff(uniqueEmail("deleted"), true);

        Staff result = staffDAO.getStaffById(insertedStaffId);

        assertNull(result);
    }

    private String uniqueEmail(String label) {
        return "it-fr10-" + label + "-" + System.nanoTime() + "@example.com";
    }

    private int insertStaff(String email, boolean deleted) throws Exception {
        String sql = "INSERT INTO Staff (EmployeeCode, FullName, Gender, DateOfBirth, PhoneNumber, "
                + "Email, PasswordHash, Department, Position, Salary, HireDate, Role_ID, IsActive, Deleted) "
                + "VALUES (?, ?, ?, CAST('1995-01-01' AS DATE), ?, ?, ?, ?, ?, ?, "
                + "CAST('2026-07-18' AS DATE), ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "IT" + System.nanoTime());
            ps.setString(2, "IT FR-10 Staff");
            ps.setBoolean(3, true);
            ps.setString(4, "0999999999");
            ps.setString(5, email);
            ps.setString(6, "unused-hash");
            ps.setString(7, "Quality");
            ps.setString(8, "Tester");
            ps.setString(9, "1000.00");
            ps.setInt(10, testRoleId);
            ps.setBoolean(11, true);
            ps.setBoolean(12, deleted);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }
}
