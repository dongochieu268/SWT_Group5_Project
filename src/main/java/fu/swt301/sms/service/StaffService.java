package fu.swt301.sms.service;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.utils.PasswordUtils;

public class StaffService {
    static final int DEFAULT_PAGE = 1;
    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 50;

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
}
