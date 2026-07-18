package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StaffServiceTest {
    @Test
    public void createStaffHashesPasswordBeforeSaving() {
        FakeStaffDAO staffDAO = new FakeStaffDAO();
        StaffService staffService = new StaffService(staffDAO);
        Staff staff = new Staff();
        staff.setPassword("staff123");

        staffService.createStaff(staff);

        assertTrue(PasswordUtils.isBCryptHash(staffDAO.savedStaff.getPassword()));
        assertTrue(PasswordUtils.verifyPassword("staff123", staffDAO.savedStaff.getPassword()));
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

    private static class FakeStaffDAO extends StaffDAO {
        private Staff savedStaff;
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
    }
}
