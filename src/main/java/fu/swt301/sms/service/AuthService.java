package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;

public class AuthService {
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final StaffDAO staffDAO;
    private final Clock clock;

    public AuthService() {
        this(new StaffDAO());
    }

    public AuthService(StaffDAO staffDAO) {
        this(staffDAO, Clock.systemDefaultZone());
    }

    public AuthService(StaffDAO staffDAO, Clock clock) {
        this.staffDAO = staffDAO;
        this.clock = clock;
    }

    /**
     * @throws AccountLockedException if the account is currently locked out under FR-03.
     * @throws AuthPersistenceException if the failed-attempt/lock state could not be persisted.
     */
    public Staff authenticate(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        Staff staff = staffDAO.findActiveStaffByEmail(email.trim());
        if (staff == null) {
            return null;
        }

        if (isLocked(staff)) {
            throw new AccountLockedException(staff.getLockUntil());
        }

        if (staff.getLockUntil() != null) {
            // A previous lock has already expired; start counting failures fresh
            // instead of carrying the stale count into this attempt.
            resetLoginFailures(staff);
        }

        if (!PasswordUtils.verifyPassword(password, staff.getPassword())) {
            registerFailedAttempt(staff);
            return null;
        }

        if (staff.getFailedLoginAttempts() > 0) {
            resetLoginFailures(staff);
        }

        staff.setPassword(null);
        return staff;
    }

    private boolean isLocked(Staff staff) {
        Timestamp lockUntil = staff.getLockUntil();
        return lockUntil != null && lockUntil.toInstant().isAfter(clock.instant());
    }

    private void registerFailedAttempt(Staff staff) {
        int newAttempts = staff.getFailedLoginAttempts() + 1;
        Timestamp lockUntil = newAttempts >= MAX_FAILED_ATTEMPTS
                ? Timestamp.from(clock.instant().plus(LOCK_DURATION))
                : null;
        try {
            staffDAO.updateLoginFailure(staff.getStaffID(), newAttempts, lockUntil);
        } catch (SQLException | ClassNotFoundException e) {
            throw new AuthPersistenceException("Failed to record failed login attempt", e);
        }
        staff.setFailedLoginAttempts(newAttempts);
        staff.setLockUntil(lockUntil);
    }

    private void resetLoginFailures(Staff staff) {
        try {
            staffDAO.resetLoginFailures(staff.getStaffID());
        } catch (SQLException | ClassNotFoundException e) {
            throw new AuthPersistenceException("Failed to reset failed login attempts", e);
        }
        staff.setFailedLoginAttempts(0);
        staff.setLockUntil(null);
    }
}
