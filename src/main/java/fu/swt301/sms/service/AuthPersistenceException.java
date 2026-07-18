package fu.swt301.sms.service;

/**
 * Thrown by {@link AuthService} when the failed-login-attempt/lockout state
 * could not be persisted to the database. Deliberately unchecked so it
 * surfaces through LoginServlet's generic RuntimeException handling (safe
 * message, no internal details leaked) instead of the lockout mechanism
 * silently failing to record the attempt.
 */
public class AuthPersistenceException extends RuntimeException {
    public AuthPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
