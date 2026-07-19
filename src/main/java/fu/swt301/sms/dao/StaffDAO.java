package fu.swt301.sms.dao;

import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the Staff entity.
 * This class provides all the necessary methods to interact with the 'Staff' table in the database.
 * It handles all CRUD (Create, Read, Update, Delete) operations as well as other specific queries.
 */
public class StaffDAO {
    private final ConnectionProvider connectionProvider;

    public StaffDAO() {
        this(DBUtils::getConnection);
    }

    public StaffDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * A private helper method to map a row from the ResultSet to a Staff object.
     * This avoids code duplication in methods that retrieve staff data.
     * It performs a JOIN with the Role table to populate the nested Role object.
     * @param rs The ResultSet cursor, positioned at the row to be mapped.
     * @return A fully populated Staff object.
     * @throws SQLException if a database access error occurs.
     */
    private Staff extractStaffFromResultSet(ResultSet rs) throws SQLException {
        Staff staff = new Staff();
        staff.setStaffID(rs.getInt("StaffID"));
        staff.setEmployeeCode(rs.getString("EmployeeCode"));
        staff.setFullName(rs.getString("FullName"));
        staff.setDepartment(rs.getString("Department"));
        staff.setGender(rs.getBoolean("Gender"));
        Date dateOfBirth = rs.getDate("DateOfBirth");
        staff.setDateOfBirth(dateOfBirth == null ? null : dateOfBirth.toLocalDate());
        staff.setPhoneNumber(rs.getString("PhoneNumber"));
        staff.setEmail(rs.getString("Email"));
        staff.setDepartment(rs.getString("Department"));
        staff.setPosition(rs.getString("Position"));
        staff.setSalary(rs.getBigDecimal("Salary"));
        Date hireDate = rs.getDate("HireDate");
        staff.setHireDate(hireDate == null ? null : hireDate.toLocalDate());
        staff.setIsActive(rs.getBoolean("IsActive"));

        Role role = new Role();
        role.setRoleID(rs.getInt("Role_ID"));
        role.setRoleName(rs.getString("Role_Name"));
        staff.setRole(role);

        return staff;
    }

    /**
     * Checks if a given email already exists in the Staff table, excluding a specific staff member.
     * This is crucial for validation during updates to prevent a user from taking another user's email.
     * @param email The email to check for existence.
     * @param currentStaffId The ID of the staff member being updated. Use 0 when creating a new staff member.
     * @return true if the email exists for another staff member, false otherwise.
     * @throws SQLException if a database access error occurs.
     * @throws ClassNotFoundException if the database driver is not found.
     */
    public boolean isEmployeeCodeExists(String employeeCode, int currentStaffId) {
        return duplicateExists("EmployeeCode", employeeCode, currentStaffId);
    }

    public boolean isEmailExists(String email, int currentStaffId) {
        return duplicateExists("Email", email, currentStaffId);
    }

    private boolean duplicateExists(String column, String value, int currentStaffId) {
        String sql = "SELECT COUNT(*) FROM Staff WHERE " + column + " = ? AND StaffID != ? AND Deleted = 0";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, currentStaffId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Unable to validate duplicate staff data", e);
        }
    }

    /**
     * Checks if a given full name already exists in the Staff table, excluding a specific staff member.
     * @param fullName The full name to check.
     * @param currentStaffId The ID of the staff member being updated. Use 0 for new staff.
     * @return true if the name exists for another staff member, false otherwise.
     * @throws SQLException if a database access error occurs.
     * @throws ClassNotFoundException if the database driver is not found.
     */
    public boolean isFullNameExists(String fullName, int currentStaffId) {
        return duplicateExists("FullName", fullName, currentStaffId);
    }

    /**
     * Checks if a given phone number already exists in the Staff table, excluding a specific staff member.
     * @param phoneNumber The phone number to check.
     * @param currentStaffId The ID of the staff member being updated. Use 0 for new staff.
     * @return true if the phone number exists for another staff member, false otherwise.
     * @throws SQLException if a database access error occurs.
     * @throws ClassNotFoundException if the database driver is not found.
     */
    public boolean isPhoneNumberExists(String phoneNumber, int currentStaffId) {
        return duplicateExists("PhoneNumber", phoneNumber, currentStaffId);
    }

    /**
     * Finds an active, non-deleted staff account by email.
     * @param email The user's email.
     * @return A populated Staff object with its password hash if found, null otherwise.
     */
    public Staff findActiveStaffByEmail(String email) {
        String sql = "SELECT s.*, r.Role_Name FROM Staff s JOIN Role r ON s.Role_ID = r.Role_ID "
                + "WHERE s.Email = ? AND s.Deleted = 0 AND s.IsActive = 1";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Staff staff = extractStaffFromResultSet(rs);
                    staff.setPassword(rs.getString("PasswordHash"));
                    return staff;
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a list of staff members based on optional filter criteria.
     * @param name A string to search for in the full name (case-insensitive). Can be null or empty.
     * @param status The active status to filter by ("true" or "false"). Can be null or empty.
     * @return A list of Staff objects matching the criteria.
     */
    public List<Staff> getStaffByFilter(String name, String status) {
        return findStaffPage(name, null, status, 0, Integer.MAX_VALUE);
    }

    public int countStaffByFilter(String keyword, String department, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Staff s WHERE s.Deleted = 0");
        List<Object> parameters = new ArrayList<>();
        appendStaffFilters(sql, parameters, keyword, department, status);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParameters(ps, parameters);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Staff> findStaffPage(String keyword, String department, String status, int offset, int pageSize) {
        List<Staff> staffList = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT s.*, r.Role_Name FROM Staff s JOIN Role r ON s.Role_ID = r.Role_ID WHERE s.Deleted = 0");
        List<Object> parameters = new ArrayList<>();
        appendStaffFilters(sql, parameters, keyword, department, status);
        sql.append(" ORDER BY s.StaffID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        parameters.add(Math.max(0, offset));
        parameters.add(Math.max(1, pageSize));

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setParameters(ps, parameters);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    staffList.add(extractStaffFromResultSet(rs));
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return staffList;
    }

    private void appendStaffFilters(StringBuilder sql, List<Object> parameters, String keyword, String department, String status) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword != null) {
            sql.append(" AND (s.FullName LIKE ? OR s.EmployeeCode LIKE ?)");
            String likeKeyword = "%" + normalizedKeyword + "%";
            parameters.add(likeKeyword);
            parameters.add(likeKeyword);
        }

        String normalizedDepartment = normalize(department);
        if (normalizedDepartment != null) {
            sql.append(" AND s.Department LIKE ?");
            parameters.add("%" + normalizedDepartment + "%");
        }

        if ("true".equalsIgnoreCase(status) || "false".equalsIgnoreCase(status)) {
            sql.append(" AND s.IsActive = ?");
            parameters.add(Boolean.parseBoolean(status));
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void setParameters(PreparedStatement ps, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object parameter = parameters.get(i);
            int parameterIndex = i + 1;
            if (parameter instanceof Boolean) {
                ps.setBoolean(parameterIndex, (Boolean) parameter);
            } else if (parameter instanceof Integer) {
                ps.setInt(parameterIndex, (Integer) parameter);
            } else {
                ps.setString(parameterIndex, String.valueOf(parameter));
            }
        }
    }

    /**
     * Inserts a new staff member into the database.
     * @param staff The Staff object containing the data to be inserted.
     */
    public void createStaff(Staff staff) {
        String sql = "INSERT INTO Staff (EmployeeCode, FullName, Gender, DateOfBirth, PhoneNumber, Email, "
                + "PasswordHash, Department, Position, Salary, HireDate, Role_ID, IsActive, Deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staff.getEmployeeCode());
            ps.setString(2, staff.getFullName());
            ps.setBoolean(3, staff.isGender());
            ps.setDate(4, Date.valueOf(staff.getDateOfBirth()));
            ps.setString(5, staff.getPhoneNumber());
            ps.setString(6, staff.getEmail());
            ps.setString(7, staff.getPassword());
            ps.setString(8, staff.getDepartment());
            ps.setString(9, staff.getPosition());
            ps.setBigDecimal(10, staff.getSalary());
            ps.setDate(11, Date.valueOf(staff.getHireDate()));
            ps.setInt(12, staff.getRole().getRoleID());
            ps.setBoolean(13, staff.isIsActive());
            ps.executeUpdate();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Unable to create staff", e);
        }
    }

    /**
     * Updates an existing staff member's information in the database.
     * The password is not updated via this method.
     * @param staff The Staff object containing the updated data. The StaffID must be set.
     */
    public void updateStaff(Staff staff) {
        String sql = "UPDATE Staff SET EmployeeCode = ?, FullName = ?, Gender = ?, DateOfBirth = ?, "
                + "PhoneNumber = ?, Email = ?, Department = ?, Position = ?, Salary = ?, HireDate = ?, "
                + "Role_ID = ?, IsActive = ? WHERE StaffID = ? AND Deleted = 0";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staff.getEmployeeCode());
            ps.setString(2, staff.getFullName());
            ps.setBoolean(3, staff.isGender());
            ps.setDate(4, Date.valueOf(staff.getDateOfBirth()));
            ps.setString(5, staff.getPhoneNumber());
            ps.setString(6, staff.getEmail());
            ps.setString(7, staff.getDepartment());
            ps.setString(8, staff.getPosition());
            ps.setBigDecimal(9, staff.getSalary());
            ps.setDate(10, Date.valueOf(staff.getHireDate()));
            ps.setInt(11, staff.getRole().getRoleID());
            ps.setBoolean(12, staff.isIsActive());
            ps.setInt(13, staff.getStaffID());
            ps.executeUpdate();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Unable to update staff", e);
        }
    }

    /**
     * Soft-deletes a staff member by marking Deleted = 1. The row is kept for
     * audit purposes; already-deleted or non-existent IDs are a no-op so this
     * method is idempotent.
     * @param staffId The ID of the staff member to delete.
     */
    public void deleteStaff(int staffId) {
        String sql = "UPDATE Staff SET Deleted = 1 WHERE StaffID = ? AND Deleted = 0";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.executeUpdate();
        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException("Unable to delete staff", e);
        }
    }

    /**
     * Retrieves a single staff member by their unique ID.
     * @param staffId The ID of the staff member to retrieve.
     * @return A populated Staff object if found, null otherwise.
     */
    public Staff getStaffById(int staffId) {
        String sql = "SELECT s.*, r.Role_Name FROM Staff s JOIN Role r ON s.Role_ID = r.Role_ID WHERE s.StaffID = ? AND s.Deleted = 0";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractStaffFromResultSet(rs);
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
