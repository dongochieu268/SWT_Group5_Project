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
        String sql = "INSERT INTO Staff (FullName, Gender, PhoneNumber, Email, PasswordHash, Role_ID, IsActive, Deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "IT FR-10 Staff");
            ps.setBoolean(2, true);
            ps.setString(3, "0999999999");
            ps.setString(4, email);
            ps.setString(5, "unused-hash");
            ps.setInt(6, testRoleId);
            ps.setBoolean(7, true);
            ps.setBoolean(8, deleted);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                return keys.getInt(1);
            }
        }
    }
}
