package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;

public class StaffService {
    private final StaffDAO staffDAO;

    public StaffService() {
        this(new StaffDAO());
    }

    public StaffService(StaffDAO staffDAO) {
        this.staffDAO = staffDAO;
    }

    public void createStaff(Staff staff) {
        staff.setPassword(PasswordUtils.hashPassword(staff.getPassword()));
        staffDAO.createStaff(staff);
    }
}
