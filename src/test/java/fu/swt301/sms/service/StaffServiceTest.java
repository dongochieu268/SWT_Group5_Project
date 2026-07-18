package fu.swt301.sms.service;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StaffServiceTest {
    @Test
    public void createStaffHashesPasswordBeforeSaving() {
        TestContext context = new TestContext();
        Staff staff = validStaff();

        context.service.createStaff(staff);

        assertNotNull(context.staffDAO.savedStaff);
        assertTrue(PasswordUtils.isBCryptHash(context.staffDAO.savedStaff.getPassword()));
        assertTrue(PasswordUtils.verifyPassword("staff123", context.staffDAO.savedStaff.getPassword()));
    }

    @Test
    public void rejectsMissingRequiredField() {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        staff.setDepartment(" ");

        expectValidation("Department is required.", () -> context.service.createStaff(staff));
    }

    @Test
    public void rejectsEachRequiredCreateFieldWhenMissing() {
        assertRequired(staff -> staff.setEmployeeCode(null), "Employee code is required.");
        assertRequired(staff -> staff.setFullName(null), "Full name is required.");
        assertRequired(staff -> staff.setDateOfBirth(null), "Date of birth is required.");
        assertRequired(staff -> staff.setPhoneNumber(null), "Phone number is required.");
        assertRequired(staff -> staff.setEmail(null), "Email is required.");
        assertRequired(staff -> staff.setPassword(null), "Password is required.");
        assertRequired(staff -> staff.setDepartment(null), "Department is required.");
        assertRequired(staff -> staff.setPosition(null), "Position is required.");
        assertRequired(staff -> staff.setSalary(null), "Salary is required.");
        assertRequired(staff -> staff.setHireDate(null), "Hire date is required.");
        assertRequired(staff -> staff.setRole(null), "Role is required.");
    }

    @Test
    public void rejectsInvalidEmployeeCode() {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        staff.setEmployeeCode("emp-01");

        expectValidation("Employee code must be", () -> context.service.createStaff(staff));
    }

    @Test
    public void acceptsEmployeeCodeBoundaryAndZeroSalary() {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        staff.setEmployeeCode("ABC");
        staff.setSalary(BigDecimal.ZERO);

        context.service.createStaff(staff);

        assertNotNull(context.staffDAO.savedStaff);
    }

    @Test
    public void rejectsPhoneNotStartingWithZeroOrNotTenDigits() {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        staff.setPhoneNumber("123456789");

        expectValidation("Phone number must be", () -> context.service.createStaff(staff));
    }

    @Test
    public void rejectsInvalidEmail() {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        staff.setEmail("invalid-email");

        expectValidation("Email format is invalid.", () -> context.service.createStaff(staff));
    }

    @Test
    public void rejectsFutureDateOfBirth() {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        staff.setDateOfBirth(LocalDate.now().plusDays(1));

        expectValidation("Date of birth must not be in the future.",
                () -> context.service.createStaff(staff));
    }

    @Test
    public void rejectsNegativeSalary() {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        staff.setSalary(new BigDecimal("-0.01"));

        expectValidation("Salary must not be negative.", () -> context.service.createStaff(staff));
    }

    @Test
    public void rejectsRoleThatDoesNotExist() {
        TestContext context = new TestContext();
        context.roleDAO.roleExists = false;

        expectValidation("Selected role does not exist.",
                () -> context.service.createStaff(validStaff()));
    }

    @Test
    public void rejectsDuplicateEmployeeCode() {
        TestContext context = new TestContext();
        context.staffDAO.employeeCodeExists = true;

        expectValidation("Employee code already exists.",
                () -> context.service.createStaff(validStaff()));
    }

    @Test
    public void rejectsDuplicateFullName() {
        TestContext context = new TestContext();
        context.staffDAO.fullNameExists = true;

        expectValidation("Full name already exists.",
                () -> context.service.createStaff(validStaff()));
    }

    @Test
    public void rejectsDuplicatePhone() {
        TestContext context = new TestContext();
        context.staffDAO.phoneExists = true;

        expectValidation("Phone number already exists.",
                () -> context.service.createStaff(validStaff()));
    }

    @Test
    public void rejectsDuplicateEmail() {
        TestContext context = new TestContext();
        context.staffDAO.emailExists = true;

        expectValidation("Email already exists.",
                () -> context.service.createStaff(validStaff()));
    }

    private void expectValidation(String expectedMessage, Runnable action) {
        try {
            action.run();
            fail("Expected StaffValidationException");
        } catch (StaffValidationException e) {
            assertTrue(e.getMessage().startsWith(expectedMessage));
        }
    }

    private void assertRequired(java.util.function.Consumer<Staff> removeField, String message) {
        TestContext context = new TestContext();
        Staff staff = validStaff();
        removeField.accept(staff);
        expectValidation(message, () -> context.service.createStaff(staff));
    }

    private Staff validStaff() {
        Staff staff = new Staff();
        staff.setEmployeeCode("EMP001");
        staff.setFullName("New Staff");
        staff.setGender(true);
        staff.setDateOfBirth(LocalDate.of(1995, 1, 1));
        staff.setPhoneNumber("0987654321");
        staff.setEmail("new.staff@example.com");
        staff.setPassword("staff123");
        staff.setDepartment("Engineering");
        staff.setPosition("Developer");
        staff.setSalary(new BigDecimal("1000.00"));
        staff.setHireDate(LocalDate.of(2026, 7, 18));
        Role role = new Role();
        role.setRoleID(2);
        staff.setRole(role);
        staff.setIsActive(true);
        return staff;
    }

    private static class TestContext {
        private final FakeStaffDAO staffDAO = new FakeStaffDAO();
        private final FakeRoleDAO roleDAO = new FakeRoleDAO();
        private final StaffService service = new StaffService(staffDAO, roleDAO);
    }

    private static class FakeStaffDAO extends StaffDAO {
        private Staff savedStaff;
        private boolean employeeCodeExists;
        private boolean fullNameExists;
        private boolean phoneExists;
        private boolean emailExists;

        @Override
        public boolean isEmployeeCodeExists(String employeeCode, int currentStaffId) {
            return employeeCodeExists;
        }

        @Override
        public boolean isFullNameExists(String fullName, int currentStaffId) {
            return fullNameExists;
        }

        @Override
        public boolean isPhoneNumberExists(String phoneNumber, int currentStaffId) {
            return phoneExists;
        }

        @Override
        public boolean isEmailExists(String email, int currentStaffId) {
            return emailExists;
        }

        @Override
        public void createStaff(Staff staff) {
            savedStaff = staff;
        }
    }

    private static class FakeRoleDAO extends RoleDAO {
        private boolean roleExists = true;

        @Override
        public boolean existsById(int roleId) {
            return roleExists;
        }
    }
}
