package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AuthServiceTest {
    @Test
    public void authenticateReturnsStaffWhenPasswordMatchesHash() {
        Staff staff = new Staff();
        staff.setEmail("admin@example.com");
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        AuthService authService = new AuthService(new FakeStaffDAO(staff));

        Staff authenticated = authService.authenticate(" admin@example.com ", "admin123");

        assertSame(staff, authenticated);
        assertEquals("admin@example.com", authenticated.getEmail());
        assertNull(authenticated.getPassword());
    }

    @Test
    public void authenticateRejectsWrongPassword() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        AuthService authService = new AuthService(new FakeStaffDAO(staff));

        assertNull(authService.authenticate("admin@example.com", "wrong-password"));
    }

    @Test
    public void authenticateRejectsMissingAccount() {
        AuthService authService = new AuthService(new FakeStaffDAO(null));

        assertNull(authService.authenticate("missing@example.com", "admin123"));
    }

    @Test
    public void authenticateRejectsNullInput() {
        AuthService authService = new AuthService(new FakeStaffDAO(new Staff()));

        assertNull(authService.authenticate(null, "admin123"));
        assertNull(authService.authenticate("admin@example.com", null));
    }

    private static class FakeStaffDAO extends StaffDAO {
        private final Staff staff;

        private FakeStaffDAO(Staff staff) {
            this.staff = staff;
        }

        @Override
        public Staff findActiveStaffByEmail(String email) {
            return staff;
        }
    }
}
