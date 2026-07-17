package fu.swt301.sms.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtils {
    private static final int WORKLOAD = 12;

    private PasswordUtils() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORKLOAD));
    }

    public static boolean verifyPassword(String plainPassword, String passwordHash) {
        if (plainPassword == null || passwordHash == null || passwordHash.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isBCryptHash(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
