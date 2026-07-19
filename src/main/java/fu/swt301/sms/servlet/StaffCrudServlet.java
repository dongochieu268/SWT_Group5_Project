package fu.swt301.sms.servlet;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;
import fu.swt301.sms.service.StaffValidationException;
import fu.swt301.sms.utils.CsrfTokenManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/staff-crud")
public class StaffCrudServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(StaffCrudServlet.class.getName());
    private static final String STAFF_LIST_REDIRECT = "staff-list";

    private final StaffDAO staffDAO;
    private final StaffService staffService;
    private final RoleDAO roleDAO;

    public StaffCrudServlet() {
        this(new StaffDAO(), new RoleDAO());
    }

    StaffCrudServlet(StaffDAO staffDAO, RoleDAO roleDAO) {
        this(staffDAO, new StaffService(staffDAO, roleDAO), roleDAO);
    }

    StaffCrudServlet(StaffDAO staffDAO, StaffService staffService, RoleDAO roleDAO) {
        this.staffDAO = staffDAO;
        this.staffService = staffService;
        this.roleDAO = roleDAO;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfTokenManager.isValid(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            handleDelete(request, response);
            return;
        }

        boolean creating = "create".equals(action);
        if (!creating && !"update".equals(action)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Staff staff = new Staff();
        try {
            populateStaff(request, creating, staff);
            if (creating) {
                staffService.createStaff(staff);
            } else {
                staffService.updateStaff(staff);
            }
            response.sendRedirect(STAFF_LIST_REDIRECT);
        } catch (StaffValidationException e) {
            forwardToForm(request, response, staff, e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to save staff.", e);
            forwardToForm(request, response, staff, "Unable to save staff. Please try again.");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int staffId;
        try {
            staffId = parsePositiveInt(request.getParameter("id"), "Staff ID is invalid.");
        } catch (StaffValidationException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        HttpSession session = request.getSession(false);
        Staff currentUser = session == null ? null : (Staff) session.getAttribute("user");
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            staffService.deleteStaff(staffId, currentUser.getStaffID());
            response.sendRedirect(STAFF_LIST_REDIRECT);
        } catch (StaffValidationException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to delete staff.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void populateStaff(HttpServletRequest request, boolean creating, Staff staff) {
        if (!creating) {
            staff.setStaffID(parsePositiveInt(request.getParameter("staffID"), "Staff ID is invalid."));
        }
        staff.setEmployeeCode(trim(request.getParameter("employeeCode")).toUpperCase(Locale.ROOT));
        staff.setFullName(trim(request.getParameter("fullName")));

        String gender = request.getParameter("gender");
        if (!"true".equals(gender) && !"false".equals(gender)) {
            throw new StaffValidationException("Gender is required.");
        }
        staff.setGender(Boolean.parseBoolean(gender));

        staff.setDateOfBirth(parseDate(request.getParameter("dateOfBirth"), "Date of birth is invalid."));
        staff.setPhoneNumber(trim(request.getParameter("phoneNumber")));
        staff.setEmail(trim(request.getParameter("email")).toLowerCase(Locale.ROOT));
        if (creating) {
            staff.setPassword(request.getParameter("password"));
        }
        staff.setDepartment(trim(request.getParameter("department")));
        staff.setPosition(trim(request.getParameter("position")));
        staff.setSalary(parseSalary(request.getParameter("salary")));
        staff.setHireDate(parseDate(request.getParameter("hireDate"), "Hire date is invalid."));

        Role role = new Role();
        role.setRoleID(parsePositiveInt(request.getParameter("roleID"), "Role is required."));
        staff.setRole(role);

        String isActive = request.getParameter("isActive");
        if (!"true".equals(isActive) && !"false".equals(isActive)) {
            throw new StaffValidationException("Status is required.");
        }
        staff.setIsActive(Boolean.parseBoolean(isActive));
    }

    private LocalDate parseDate(String value, String message) {
        try {
            return LocalDate.parse(trim(value));
        } catch (DateTimeParseException e) {
            throw new StaffValidationException(message);
        }
    }

    private BigDecimal parseSalary(String value) {
        try {
            return new BigDecimal(trim(value));
        } catch (NumberFormatException e) {
            throw new StaffValidationException("Salary is invalid.");
        }
    }

    private int parsePositiveInt(String value, String message) {
        try {
            int parsed = Integer.parseInt(trim(value));
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new StaffValidationException(message);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void forwardToForm(HttpServletRequest request, HttpServletResponse response,
            Staff staff, String errorMessage) throws ServletException, IOException {
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("staff", staff);
        request.setAttribute("roleList", roleDAO.getAllRoles());
        request.setAttribute("today", LocalDate.now());
        CsrfTokenManager.ensureToken(request);
        request.getRequestDispatcher("staff-form.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
        request.setAttribute("today", LocalDate.now());

        if ("edit".equals(action)) {
            int staffId;
            try {
                staffId = parsePositiveInt(request.getParameter("id"), "Staff ID is invalid.");
            } catch (StaffValidationException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
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
}
