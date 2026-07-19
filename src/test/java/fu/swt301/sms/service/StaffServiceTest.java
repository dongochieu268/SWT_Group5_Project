package fu.swt301.sms.service;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
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

    @Test
    public void updateStaffPersistsChangesWhenStaffExists() {
        TestContext context = new TestContext();
        Staff existing = new Staff();
        existing.setStaffID(7);
        context.staffDAO.staffToReturn = existing;
        Staff updated = validStaff();
        updated.setStaffID(7);

        context.service.updateStaff(updated);

        assertSame(updated, context.staffDAO.savedStaff);
    }

    @Test
    public void updateStaffThrowsWhenStaffIdDoesNotExistOrIsDeleted() {
        TestContext context = new TestContext();
        Staff updated = validStaff();
        updated.setStaffID(999);
        // context.staffDAO.staffToReturn stays null, simulating a missing/soft-deleted StaffID.

        try {
            context.service.updateStaff(updated);
            fail("Expected StaffNotFoundException");
        } catch (StaffNotFoundException expected) {
            assertEquals(999, expected.getStaffId());
        }
        assertFalse("existence must be checked before running duplicate-check queries",
                context.staffDAO.duplicateCheckCalled);
        assertFalse("update must not be attempted when the target does not exist",
                context.staffDAO.updateCalled);
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

    @Test
    public void getStaffPageNormalizesInvalidPageAndPageSize() {
        FakeStaffDAO staffDAO = new FakeStaffDAO();
        staffDAO.totalItems = 2;
        staffDAO.pageResult = Arrays.asList(new Staff(), new Staff());
        StaffService staffService = new StaffService(staffDAO);

        StaffPage staffPage = staffService.getStaffPage(null, null, null, "abc", "-3");

        assertEquals(1, staffPage.getCurrentPage());
        assertEquals(10, staffPage.getPageSize());
        assertEquals(0, staffDAO.receivedOffset);
        assertEquals(10, staffDAO.receivedPageSize);
        assertEquals(1, staffPage.getTotalPages());
    }

    @Test
    public void getStaffPageCapsPageSizeAndMovesPastLastPageToLastPage() {
        FakeStaffDAO staffDAO = new FakeStaffDAO();
        staffDAO.totalItems = 95;
        staffDAO.pageResult = new ArrayList<Staff>();
        StaffService staffService = new StaffService(staffDAO);

        StaffPage staffPage = staffService.getStaffPage(null, null, null, "99", "500");

        assertEquals(2, staffPage.getCurrentPage());
        assertEquals(50, staffPage.getPageSize());
        assertEquals(50, staffDAO.receivedOffset);
        assertEquals(50, staffDAO.receivedPageSize);
        assertEquals(2, staffPage.getTotalPages());
    }

    @Test
    public void getStaffPageKeepsFiltersForCountAndPageQuery() {
        FakeStaffDAO staffDAO = new FakeStaffDAO();
        staffDAO.totalItems = 25;
        staffDAO.pageResult = Arrays.asList(new Staff());
        StaffService staffService = new StaffService(staffDAO);

        staffService.getStaffPage("NV001", "IT", "true", "2", "10");

        assertEquals("NV001", staffDAO.countKeyword);
        assertEquals("IT", staffDAO.countDepartment);
        assertEquals("true", staffDAO.countStatus);
        assertEquals("NV001", staffDAO.pageKeyword);
        assertEquals("IT", staffDAO.pageDepartment);
        assertEquals("true", staffDAO.pageStatus);
        assertEquals(10, staffDAO.receivedOffset);
    }

    @Test
    public void getStaffPageUsesFirstPageWhenNoDataExists() {
        FakeStaffDAO staffDAO = new FakeStaffDAO();
        staffDAO.totalItems = 0;
        StaffService staffService = new StaffService(staffDAO);

        StaffPage staffPage = staffService.getStaffPage(null, null, null, "5", "10");

        assertEquals(1, staffPage.getCurrentPage());
        assertEquals(0, staffPage.getTotalPages());
        assertEquals(0, staffDAO.receivedOffset);
    }

    @Test
    public void getStaffByIdDelegatesToDao() {
        Staff expectedStaff = new Staff();
        FakeStaffDAO staffDAO = new FakeStaffDAO();
        staffDAO.staffToReturn = expectedStaff;
        StaffService staffService = new StaffService(staffDAO);

        Staff result = staffService.getStaffById(1);

        assertSame(expectedStaff, result);
    }

    private static class FakeStaffDAO extends StaffDAO {
        private Staff savedStaff;
        private boolean employeeCodeExists;
        private boolean fullNameExists;
        private boolean phoneExists;
        private boolean emailExists;
        private Staff staffToReturn;
        private int totalItems;
        private List<Staff> pageResult = new ArrayList<Staff>();
        private String countKeyword;
        private String countDepartment;
        private String countStatus;
        private String pageKeyword;
        private String pageDepartment;
        private String pageStatus;
        private int receivedOffset;
        private int receivedPageSize;
        private boolean duplicateCheckCalled;
        private boolean updateCalled;

        @Override
        public boolean isEmployeeCodeExists(String employeeCode, int currentStaffId) {
            duplicateCheckCalled = true;
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

        @Override
        public int countStaffByFilter(String keyword, String department, String status) {
            countKeyword = keyword;
            countDepartment = department;
            countStatus = status;
            return totalItems;
        }

        @Override
        public List<Staff> findStaffPage(String keyword, String department, String status, int offset, int pageSize) {
            pageKeyword = keyword;
            pageDepartment = department;
            pageStatus = status;
            receivedOffset = offset;
            receivedPageSize = pageSize;
            return pageResult;
        }

        @Override
        public Staff getStaffById(int staffId) {
            return staffToReturn;
        }

        @Override
        public void updateStaff(Staff staff) {
            updateCalled = true;
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
