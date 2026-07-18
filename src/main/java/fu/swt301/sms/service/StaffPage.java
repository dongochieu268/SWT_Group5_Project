package fu.swt301.sms.service;

import fu.swt301.sms.entity.Staff;

import java.util.Collections;
import java.util.List;

public class StaffPage {
    private final List<Staff> staffList;
    private final int currentPage;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;

    public StaffPage(List<Staff> staffList, int currentPage, int pageSize, int totalItems) {
        this.staffList = staffList == null ? Collections.<Staff>emptyList() : staffList;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = Math.max(0, totalItems);
        this.totalPages = this.totalItems == 0 ? 0 : (int) Math.ceil((double) this.totalItems / pageSize);
    }

    public List<Staff> getStaffList() {
        return staffList;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isHasPrevious() {
        return currentPage > 1;
    }

    public boolean isHasNext() {
        return totalPages > 0 && currentPage < totalPages;
    }
}
