package fu.swt301.sms.service;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class StaffService {
    private static final Pattern EMPLOYEE_CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9]{2,19}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("0\\d{9}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    static final int DEFAULT_PAGE = 1;
    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 50;

    private final StaffDAO staffDAO;
    private final RoleDAO roleDAO;

    public StaffService() {
        this(new StaffDAO(), new RoleDAO());
    }

    public StaffService(StaffDAO staffDAO) {
        this(staffDAO, new RoleDAO());
    }

    public StaffService(StaffDAO staffDAO, RoleDAO roleDAO) {
        this.staffDAO = staffDAO;
        this.roleDAO = roleDAO;
    }

    public void createStaff(Staff staff) {
        validateStaff(staff, true);
        validateUniqueFields(staff);
        validateRole(staff);
        staff.setPassword(PasswordUtils.hashPassword(staff.getPassword()));
        staffDAO.createStaff(staff);
    }

    public void updateStaff(Staff staff) {
        validateStaff(staff, false);
        validateUniqueFields(staff);
        validateRole(staff);
        staffDAO.updateStaff(staff);
    }

    private void validateStaff(Staff staff, boolean creating) {
        if (staff == null) {
            throw new StaffValidationException("Staff data is required.");
        }
        requireText(staff.getEmployeeCode(), "Employee code is required.");
        if (!EMPLOYEE_CODE_PATTERN.matcher(staff.getEmployeeCode()).matches()) {
            throw new StaffValidationException(
                    "Employee code must be 3-20 uppercase letters or digits and start with a letter.");
        }
        requireText(staff.getFullName(), "Full name is required.");
        if (staff.getFullName().length() > 100) {
            throw new StaffValidationException("Full name must not exceed 100 characters.");
        }
        if (staff.getDateOfBirth() == null) {
            throw new StaffValidationException("Date of birth is required.");
        }
        if (staff.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new StaffValidationException("Date of birth must not be in the future.");
        }
        requireText(staff.getPhoneNumber(), "Phone number is required.");
        if (!PHONE_PATTERN.matcher(staff.getPhoneNumber()).matches()) {
            throw new StaffValidationException("Phone number must be 10 digits and start with 0.");
        }
        requireText(staff.getEmail(), "Email is required.");
        if (!EMAIL_PATTERN.matcher(staff.getEmail()).matches()) {
            throw new StaffValidationException("Email format is invalid.");
        }
        if (staff.getEmail().length() > 100) {
            throw new StaffValidationException("Email must not exceed 100 characters.");
        }
        if (creating) {
            requireText(staff.getPassword(), "Password is required.");
            if (staff.getPassword().length() > 72) {
                throw new StaffValidationException("Password must not exceed 72 characters.");
            }
        }
        requireText(staff.getDepartment(), "Department is required.");
        if (staff.getDepartment().length() > 100) {
            throw new StaffValidationException("Department must not exceed 100 characters.");
        }
        requireText(staff.getPosition(), "Position is required.");
        if (staff.getPosition().length() > 100) {
            throw new StaffValidationException("Position must not exceed 100 characters.");
        }
        BigDecimal salary = staff.getSalary();
        if (salary == null) {
            throw new StaffValidationException("Salary is required.");
        }
        if (salary.signum() < 0) {
            throw new StaffValidationException("Salary must not be negative.");
        }
        if (staff.getHireDate() == null) {
            throw new StaffValidationException("Hire date is required.");
        }
        if (staff.getRole() == null || staff.getRole().getRoleID() <= 0) {
            throw new StaffValidationException("Role is required.");
        }
    }

    private void validateUniqueFields(Staff staff) {
        int staffId = staff.getStaffID();
        if (staffDAO.isEmployeeCodeExists(staff.getEmployeeCode(), staffId)) {
            throw new StaffValidationException("Employee code already exists.");
        }
        if (staffDAO.isFullNameExists(staff.getFullName(), staffId)) {
            throw new StaffValidationException("Full name already exists.");
        }
        if (staffDAO.isPhoneNumberExists(staff.getPhoneNumber(), staffId)) {
            throw new StaffValidationException("Phone number already exists.");
        }
        if (staffDAO.isEmailExists(staff.getEmail(), staffId)) {
            throw new StaffValidationException("Email already exists.");
        }
    }

    private void validateRole(Staff staff) {
        if (!roleDAO.existsById(staff.getRole().getRoleID())) {
            throw new StaffValidationException("Selected role does not exist.");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new StaffValidationException(message);
        }
    }

    public StaffPage getStaffPage(String keyword, String department, String status, String pageParam, String pageSizeParam) {
        int pageSize = normalizePageSize(pageSizeParam);
        int requestedPage = normalizePositiveInt(pageParam, DEFAULT_PAGE);
        int totalItems = staffDAO.countStaffByFilter(keyword, department, status);
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        int currentPage = totalPages == 0 ? DEFAULT_PAGE : Math.min(requestedPage, totalPages);
        int offset = (currentPage - 1) * pageSize;

        return new StaffPage(
                staffDAO.findStaffPage(keyword, department, status, offset, pageSize),
                currentPage,
                pageSize,
                totalItems
        );
    }

    private int normalizePageSize(String pageSizeParam) {
        int pageSize = normalizePositiveInt(pageSizeParam, DEFAULT_PAGE_SIZE);
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int normalizePositiveInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Staff getStaffById(int staffId) {
        return staffDAO.getStaffById(staffId);
    }

    /**
     * Soft-deletes the given staff member.
     * @param staffId the staff to delete.
     * @param currentStaffId the StaffID of the logged-in user performing the action.
     * @throws StaffValidationException if staffId matches currentStaffId - a user
     * cannot delete their own account while logged in.
     */
    public void deleteStaff(int staffId, int currentStaffId) {
        if (staffId == currentStaffId) {
            throw new StaffValidationException("You cannot delete your own account while logged in.");
        }
        staffDAO.deleteStaff(staffId);
    }
}
