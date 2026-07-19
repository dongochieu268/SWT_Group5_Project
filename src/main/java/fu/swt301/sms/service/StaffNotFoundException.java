package fu.swt301.sms.service;

/**
 * Thrown when an operation targets a StaffID that does not exist or has been
 * soft-deleted (Deleted = 1). Callers should translate this into HTTP 404.
 */
public class StaffNotFoundException extends RuntimeException {
    private final int staffId;

    public StaffNotFoundException(int staffId) {
        super("Staff not found: " + staffId);
        this.staffId = staffId;
    }

    public int getStaffId() {
        return staffId;
    }
}
