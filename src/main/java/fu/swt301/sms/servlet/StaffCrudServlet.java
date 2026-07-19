package fu.swt301.sms.servlet;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;
import fu.swt301.sms.utils.CsrfTokenManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet acts as a controller for all CRUD (Create, Read, Update, Delete) operations related to Staff.
 * It handles both the display of forms (for creating/editing) and the processing of submitted form data.
 */
@WebServlet("/staff-crud")
public class StaffCrudServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(StaffCrudServlet.class.getName());
    private final StaffDAO staffDAO;
    private final StaffService staffService;
    private final RoleDAO roleDAO;

    public StaffCrudServlet() {
        this(new StaffDAO(), new RoleDAO());
    }

    StaffCrudServlet(StaffDAO staffDAO, RoleDAO roleDAO) {
        this(staffDAO, new StaffService(staffDAO), roleDAO);
    }

    StaffCrudServlet(StaffDAO staffDAO, StaffService staffService, RoleDAO roleDAO) {
        this.staffDAO = staffDAO;
        this.staffService = staffService;
        this.roleDAO = roleDAO;
    }

    /**
     * Handles POST requests, which are used to submit data for creating or updating a staff member.
     * This method contains the core logic for data validation and persistence.
     * @param request The HttpServletRequest object containing the form data.
     * @param response The HttpServletResponse object for sending the response.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!CsrfTokenManager.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            Integer staffId = parsePositiveInteger(request.getParameter("id"));
            if (staffId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            staffDAO.deleteStaff(staffId);
            response.sendRedirect("staff-list");
            return;
        }

        if (!"create".equals(action) && !"update".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Staff staff;
        try {
            // Input strings are normalized before validation and persistence.
            staff = buildStaffFromRequest(request, action);

            if (staffDAO.isEmailExists(staff.getEmail(), staff.getStaffID())) {
                forwardFormWithError(request, response, roleDAO, staff, "Email already exists. Please choose another one.");
                return;
            } else if (staffDAO.isFullNameExists(staff.getFullName(), staff.getStaffID())) {
                forwardFormWithError(request, response, roleDAO, staff, "Full name already exists. Please choose another one.");
                return;
            } else if (staffDAO.isPhoneNumberExists(staff.getPhoneNumber(), staff.getStaffID())) {
                forwardFormWithError(request, response, roleDAO, staff, "Phone number already exists. Please choose another one.");
                return;
            }
        } catch (IllegalArgumentException e) {
            forwardFormWithError(request, response, roleDAO, buildStaffSnapshot(request, action), e.getMessage());
            return;
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Database error during staff validation.", e);
            forwardFormWithError(request, response, roleDAO, buildStaffSnapshot(request, action), "Database error during validation.");
            return;
        }

        if ("create".equals(action)) {
            staffService.createStaff(staff);
        } else {
            staffDAO.updateStaff(staff);
        }
        response.sendRedirect("staff-list");
    }

    /**
     * Handles GET requests, which are used to display pages or perform simple actions like deletion.
     * @param request The HttpServletRequest object.
     * @param response The HttpServletResponse object.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (action != null && !"create".equals(action) && !"edit".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        List<Role> roleList = roleDAO.getAllRoles();
        request.setAttribute("roleList", roleList);

        if ("edit".equals(action)) {
            Integer staffId = parsePositiveInteger(request.getParameter("id"));
            if (staffId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            Staff staff = staffDAO.getStaffById(staffId);
            if (staff == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("staff", staff);
        }

        CsrfTokenManager.ensureToken(request);
        request.getRequestDispatcher("staff-form.jsp").forward(request, response);
    }

    private Staff buildStaffFromRequest(HttpServletRequest request, String action) {
        Staff staff = buildStaffSnapshot(request, action);
        if ("update".equals(action) && staff.getStaffID() <= 0) {
            throw new IllegalArgumentException("Invalid staff ID.");
        }

        staff.setFullName(requiredTrimmed(request, "fullName", "Full name is required.", 100));
        staff.setGender(requiredBoolean(request, "gender", "Gender is required."));
        staff.setPhoneNumber(requiredPhoneNumber(request));
        staff.setEmail(requiredEmail(request));
        staff.setIsActive(requiredBoolean(request, "isActive", "Status is required."));

        Integer roleId = parsePositiveInteger(request.getParameter("roleID"));
        if (roleId == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        Role role = new Role();
        role.setRoleID(roleId);
        staff.setRole(role);

        if ("create".equals(action)) {
            String password = request.getParameter("password");
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required.");
            }
            if (password.length() > 50) {
                throw new IllegalArgumentException("Password must be 50 characters or fewer.");
            }
            staff.setPassword(password);
        }
        return staff;
    }

    private Staff buildStaffSnapshot(HttpServletRequest request, String action) {
        Staff staff = new Staff();
        if ("update".equals(action)) {
            Integer staffId = parsePositiveInteger(request.getParameter("staffID"));
            staff.setStaffID(staffId == null ? 0 : staffId);
        }

        staff.setFullName(trimToEmpty(request.getParameter("fullName")));
        staff.setPhoneNumber(trimToEmpty(request.getParameter("phoneNumber")));
        staff.setEmail(trimToEmpty(request.getParameter("email")));
        staff.setGender(Boolean.parseBoolean(request.getParameter("gender")));
        staff.setIsActive(Boolean.parseBoolean(request.getParameter("isActive")));

        Integer roleId = parsePositiveInteger(request.getParameter("roleID"));
        if (roleId != null) {
            Role role = new Role();
            role.setRoleID(roleId);
            staff.setRole(role);
        }
        return staff;
    }

    private void forwardFormWithError(HttpServletRequest request, HttpServletResponse response,
            RoleDAO roleDAO, Staff staff, String errorMessage) throws ServletException, IOException {
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("staff", staff);
        request.setAttribute("roleList", roleDAO.getAllRoles());
        CsrfTokenManager.ensureToken(request);
        request.getRequestDispatcher("staff-form.jsp").forward(request, response);
    }

    private String requiredTrimmed(HttpServletRequest request, String parameterName, String errorMessage, int maxLength) {
        String value = trimToEmpty(request.getParameter(parameterName));
        if (value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(errorMessage.replace(" is required.", "") + " is too long.");
        }
        return value;
    }

    private String requiredPhoneNumber(HttpServletRequest request) {
        String phoneNumber = requiredTrimmed(request, "phoneNumber", "Phone number is required.", 10);
        if (!phoneNumber.matches("0[0-9]{9}")) {
            throw new IllegalArgumentException("Phone number must be 10 digits and start with 0.");
        }
        return phoneNumber;
    }

    private String requiredEmail(HttpServletRequest request) {
        String email = requiredTrimmed(request, "email", "Email is required.", 100);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email format is invalid.");
        }
        return email;
    }

    private boolean requiredBoolean(HttpServletRequest request, String parameterName, String errorMessage) {
        String value = request.getParameter(parameterName);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return Boolean.parseBoolean(value);
    }

    private Integer parsePositiveInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue > 0 ? parsedValue : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
