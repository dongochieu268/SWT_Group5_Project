package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;

public class AuthService {
    private final StaffDAO staffDAO;

    public AuthService() {
        this(new StaffDAO());
    }

    public AuthService(StaffDAO staffDAO) {
        this.staffDAO = staffDAO;
    }

    public Staff authenticate(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        Staff staff = staffDAO.findActiveStaffByEmail(email.trim());
        if (staff == null || !PasswordUtils.verifyPassword(password, staff.getPassword())) {
            return null;
        }

        staff.setPassword(null);
        return staff;
    }
}
