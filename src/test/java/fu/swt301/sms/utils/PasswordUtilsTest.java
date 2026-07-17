package fu.swt301.sms.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordUtilsTest {
    @Test
    public void hashPasswordDoesNotReturnPlaintext() {
        String hash = PasswordUtils.hashPassword("admin123");

        assertNotEquals("admin123", hash);
        assertTrue(PasswordUtils.isBCryptHash(hash));
    }

    @Test
    public void verifyPasswordAcceptsCorrectPassword() {
        String hash = PasswordUtils.hashPassword("admin123");

        assertTrue(PasswordUtils.verifyPassword("admin123", hash));
    }

    @Test
    public void verifyPasswordRejectsWrongPassword() {
        String hash = PasswordUtils.hashPassword("admin123");

        assertFalse(PasswordUtils.verifyPassword("wrong-password", hash));
    }

    @Test
    public void verifyPasswordRejectsInvalidHashSafely() {
        assertFalse(PasswordUtils.verifyPassword("admin123", "not-a-bcrypt-hash"));
        assertFalse(PasswordUtils.verifyPassword("admin123", null));
        assertFalse(PasswordUtils.verifyPassword(null, PasswordUtils.hashPassword("admin123")));
    }
}
