package fu.swt301.sms.service;

import fu.swt301.sms.dao.ConnectionProvider;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration Test for FR-03: verifies FailedLoginAttempts/LockUntil are
 * actually persisted to and read back from a real SQL database (H2 in
 * MSSQLServer compatibility mode, same convention as StaffDAOIT), not just
 * tracked in memory by a fake DAO.
 */
public class AuthServiceIT {
    private static final String URL =
            "jdbc:h2:mem:fr3;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
    private static final String EMAIL = "lockout.it@example.com";
    private static final String PASSWORD = "admin123";

    private ConnectionProvider connectionProvider;
    private StaffDAO staffDAO;
    private int staffId;

    @Before
    public void setUpDatabase() throws Exception {
        Class.forName("org.h2.Driver");
        connectionProvider = () -> DriverManager.getConnection(URL, "sa", "");
        try (Connection conn = connectionProvider.getConnection()) {
            execute(conn, "CREATE TABLE Role (RoleID INT PRIMARY KEY, RoleName VARCHAR(50) NOT NULL)");
            execute(conn,
                    "CREATE TABLE Staff ("
                            + "StaffID INT IDENTITY(1,1) PRIMARY KEY,"
                            + "EmployeeCode VARCHAR(50),"
                            + "FullName VARCHAR(100) NOT NULL,"
                            + "Department VARCHAR(100),"
                            + "Gender BIT NOT NULL,"
                            + "PhoneNumber VARCHAR(20),"
                            + "Email VARCHAR(100) NOT NULL,"
                            + "PasswordHash VARCHAR(255) NOT NULL,"
                            + "RoleID INT NOT NULL,"
                            + "IsActive BIT NOT NULL,"
                            + "Deleted BIT NOT NULL DEFAULT 0,"
                            + "FailedLoginAttempts INT NOT NULL DEFAULT 0,"
                            + "LockUntil TIMESTAMP NULL,"
                            + "FOREIGN KEY (RoleID) REFERENCES Role(RoleID))");
            execute(conn, "INSERT INTO Role (RoleID, RoleName) VALUES (2, 'User')");
        }

        staffDAO = new StaffDAO(connectionProvider);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO Staff (FullName, Gender, PhoneNumber, Email, PasswordHash, RoleID, IsActive) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "Lockout IT Staff");
            ps.setBoolean(2, true);
            ps.setString(3, "0987654321");
            ps.setString(4, EMAIL);
            ps.setString(5, PasswordUtils.hashPassword(PASSWORD));
            ps.setInt(6, 2);
            ps.setBoolean(7, true);
            ps.executeUpdate();
        }
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT StaffID FROM Staff WHERE Email = ?")) {
            ps.setString(1, EMAIL);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                staffId = rs.getInt(1);
            }
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
    public void fifthFailedAttemptPersistsLockUntilToTheDatabase() {
        Instant now = Instant.parse("2026-07-19T10:00:00Z");
        AuthService authService = new AuthService(staffDAO, Clock.fixed(now, ZoneOffset.UTC));

        for (int i = 0; i < 5; i++) {
            assertNull(authService.authenticate(EMAIL, "wrong-password"));
        }

        StoredState state = readState();
        assertEquals(5, state.failedLoginAttempts);
        assertNotNull("LockUntil must be persisted to the database", state.lockUntil);
        assertEquals(Timestamp.from(now.plusSeconds(300)), state.lockUntil);
    }

    @Test
    public void lockedAccountReadFromDatabaseRejectsEvenCorrectPassword() {
        Instant lockSetAt = Instant.parse("2026-07-19T10:00:00Z");
        AuthService lockingService = new AuthService(staffDAO, Clock.fixed(lockSetAt, ZoneOffset.UTC));
        for (int i = 0; i < 5; i++) {
            lockingService.authenticate(EMAIL, "wrong-password");
        }

        // A brand new AuthService instance simulates a fresh HTTP request that
        // must read the lock state back from the database, not from memory.
        Instant stillLocked = lockSetAt.plusSeconds(60);
        AuthService secondRequest = new AuthService(staffDAO, Clock.fixed(stillLocked, ZoneOffset.UTC));

        try {
            secondRequest.authenticate(EMAIL, PASSWORD);
            fail("Expected AccountLockedException");
        } catch (AccountLockedException expected) {
            assertNotNull(expected.getLockUntil());
        }
    }

    @Test
    public void afterLockExpiresDatabaseStateIsClearedOnSuccessfulLogin() {
        Instant lockSetAt = Instant.parse("2026-07-19T10:00:00Z");
        AuthService lockingService = new AuthService(staffDAO, Clock.fixed(lockSetAt, ZoneOffset.UTC));
        for (int i = 0; i < 5; i++) {
            lockingService.authenticate(EMAIL, "wrong-password");
        }

        Instant afterExpiry = lockSetAt.plusSeconds(301);
        AuthService laterRequest = new AuthService(staffDAO, Clock.fixed(afterExpiry, ZoneOffset.UTC));
        Staff authenticated = laterRequest.authenticate(EMAIL, PASSWORD);

        assertNotNull("Correct password after lock expiry must succeed", authenticated);
        StoredState state = readState();
        assertEquals(0, state.failedLoginAttempts);
        assertNull("LockUntil must be cleared in the database", state.lockUntil);
    }

    private StoredState readState() {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT FailedLoginAttempts, LockUntil FROM Staff WHERE StaffID = ?")) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                StoredState state = new StoredState();
                state.failedLoginAttempts = rs.getInt("FailedLoginAttempts");
                state.lockUntil = rs.getTimestamp("LockUntil");
                return state;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void execute(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private static class StoredState {
        private int failedLoginAttempts;
        private Timestamp lockUntil;
    }
}
