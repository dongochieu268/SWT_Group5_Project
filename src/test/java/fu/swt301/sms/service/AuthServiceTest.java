package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-18T10:00:00Z");

    @Test
    public void authenticateReturnsStaffWhenPasswordMatchesHash() {
        Staff staff = new Staff();
        staff.setEmail("admin@example.com");
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(NOW));

        Staff authenticated = authService.authenticate(" admin@example.com ", "admin123");

        assertSame(staff, authenticated);
        assertEquals("admin@example.com", authenticated.getEmail());
        assertNull(authenticated.getPassword());
    }

    @Test
    public void authenticateRejectsWrongPassword() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(NOW));

        assertNull(authService.authenticate("admin@example.com", "wrong-password"));
    }

    @Test
    public void authenticateRejectsMissingAccount() {
        AuthService authService = new AuthService(new FakeStaffDAO(null), fixedClock(NOW));

        assertNull(authService.authenticate("missing@example.com", "admin123"));
    }

    @Test
    public void authenticateRejectsNullInput() {
        AuthService authService = new AuthService(new FakeStaffDAO(new Staff()), fixedClock(NOW));

        assertNull(authService.authenticate(null, "admin123"));
        assertNull(authService.authenticate("admin@example.com", null));
    }

    @Test
    public void doesNotLockAccountBeforeFifthFailedAttempt() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(NOW));

        for (int i = 0; i < 4; i++) {
            assertNull(authService.authenticate("admin@example.com", "wrong-password"));
        }

        assertEquals(4, staff.getFailedLoginAttempts());
        assertNull(staff.getLockUntil());
    }

    @Test
    public void locksAccountAfterFifthFailedAttempt() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(NOW));

        for (int i = 0; i < 5; i++) {
            assertNull(authService.authenticate("admin@example.com", "wrong-password"));
        }

        assertEquals(5, staff.getFailedLoginAttempts());
        assertEquals(Timestamp.from(NOW.plus(Duration.ofMinutes(5))), staff.getLockUntil());
    }

    @Test
    public void deniesLoginDuringLockPeriodEvenWithCorrectPassword() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        staff.setFailedLoginAttempts(5);
        staff.setLockUntil(Timestamp.from(NOW.plus(Duration.ofMinutes(5))));
        Instant stillLocked = NOW.plus(Duration.ofMinutes(1));
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(stillLocked));

        try {
            authService.authenticate("admin@example.com", "admin123");
            fail("Expected AccountLockedException");
        } catch (AccountLockedException expected) {
            assertEquals(staff.getLockUntil(), expected.getLockUntil());
        }
    }

    @Test
    public void allowsLoginAfterLockExpiresAndResetsFailureCount() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        staff.setFailedLoginAttempts(5);
        staff.setLockUntil(Timestamp.from(NOW.plus(Duration.ofMinutes(5))));
        Instant afterLockExpires = NOW.plus(Duration.ofMinutes(5)).plusSeconds(1);
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(afterLockExpires));

        Staff authenticated = authService.authenticate("admin@example.com", "admin123");

        assertSame(staff, authenticated);
        assertEquals(0, staff.getFailedLoginAttempts());
        assertNull(staff.getLockUntil());
    }

    @Test
    public void successfulLoginResetsPriorFailedAttempts() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        staff.setFailedLoginAttempts(3);
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(NOW));

        assertTrue(authService.authenticate("admin@example.com", "admin123") != null);
        assertEquals(0, staff.getFailedLoginAttempts());
        assertFalse(staff.getFailedLoginAttempts() > 0);
    }

    @Test
    public void resetsFailedAttemptsWhenExpiredLockIsFollowedByAnotherWrongPassword() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        staff.setFailedLoginAttempts(5);
        staff.setLockUntil(Timestamp.from(NOW.plus(Duration.ofMinutes(5))));
        Instant afterLockExpires = NOW.plus(Duration.ofMinutes(5)).plusSeconds(1);
        AuthService authService = new AuthService(new FakeStaffDAO(staff), fixedClock(afterLockExpires));

        // The lock already expired, so a single fresh wrong attempt must not
        // immediately relock the account by counting from the stale total of 5.
        assertNull(authService.authenticate("admin@example.com", "wrong-password"));

        assertEquals(1, staff.getFailedLoginAttempts());
        assertNull(staff.getLockUntil());
    }

    @Test
    public void wrapsDatabaseFailureWhenRecordingFailedAttempt() {
        Staff staff = new Staff();
        staff.setPassword(PasswordUtils.hashPassword("admin123"));
        FakeStaffDAO staffDAO = new FakeStaffDAO(staff);
        staffDAO.failUpdatesWith(new SQLException("connection lost"));
        AuthService authService = new AuthService(staffDAO, fixedClock(NOW));

        try {
            authService.authenticate("admin@example.com", "wrong-password");
            fail("Expected AuthPersistenceException");
        } catch (AuthPersistenceException expected) {
            assertTrue(expected.getCause() instanceof SQLException);
        }
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    /**
     * A fake DAO that mutates the same Staff instance it was seeded with, so
     * consecutive authenticate() calls in a test see previously persisted
     * failed-attempt/lock state, mirroring what a real database round-trip does.
     */
    private static class FakeStaffDAO extends StaffDAO {
        private final Staff staff;
        private SQLException failureToThrow;

        private FakeStaffDAO(Staff staff) {
            this.staff = staff;
        }

        private void failUpdatesWith(SQLException failureToThrow) {
            this.failureToThrow = failureToThrow;
        }

        @Override
        public Staff findActiveStaffByEmail(String email) {
            return staff;
        }

        @Override
        public void updateLoginFailure(int staffId, int failedAttempts, Timestamp lockUntil) throws SQLException {
            if (failureToThrow != null) {
                throw failureToThrow;
            }
            staff.setFailedLoginAttempts(failedAttempts);
            staff.setLockUntil(lockUntil);
        }

        @Override
        public void resetLoginFailures(int staffId) throws SQLException {
            if (failureToThrow != null) {
                throw failureToThrow;
            }
            staff.setFailedLoginAttempts(0);
            staff.setLockUntil(null);
        }
    }
}
