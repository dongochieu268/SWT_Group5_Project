package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;
import org.junit.Test;

import static org.junit.Assert.assertSame;
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
        private Staff staffToReturn;

        @Override
        public void createStaff(Staff staff) {
            savedStaff = staff;
        }

        @Override
        public Staff getStaffById(int staffId) {
            return staffToReturn;
        }
    }
}
