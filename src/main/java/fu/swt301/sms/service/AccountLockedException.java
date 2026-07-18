package fu.swt301.sms.service;

import java.sql.Timestamp;

/**
 * Thrown by {@link AuthService#authenticate} when the account exists and the
 * credentials would otherwise be checked, but it is currently locked out
 * under FR-03. Callers must show a distinct "account locked" message instead
 * of the generic invalid-credentials message (FR-02 explicitly allows this).
 */
public class AccountLockedException extends RuntimeException {
    private final Timestamp lockUntil;

    public AccountLockedException(Timestamp lockUntil) {
        super("Account is locked until " + lockUntil);
        this.lockUntil = lockUntil;
    }

    public Timestamp getLockUntil() {
        return lockUntil;
    }
}
