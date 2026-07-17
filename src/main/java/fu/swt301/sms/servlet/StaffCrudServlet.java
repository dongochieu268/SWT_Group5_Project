package fu.swt301.sms.servlet;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * This servlet acts as a controller for all CRUD (Create, Read, Update, Delete) operations related to Staff.
 * It handles both the display of forms (for creating/editing) and the processing of submitted form data.
 */
@WebServlet("/staff-crud")
public class StaffCrudServlet extends HttpServlet {

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
        String action = request.getParameter("action");
        StaffDAO staffDAO = new StaffDAO();
        StaffService staffService = new StaffService(staffDAO);
        RoleDAO roleDAO = new RoleDAO();

        // --- Step 1: Populate a Staff object from the request parameters ---
        // Input strings are trimmed to remove leading/trailing whitespace for data consistency.
        Staff staff = new Staff();
        String staffIdParam = request.getParameter("staffID");
        int staffId = (staffIdParam != null && !staffIdParam.isEmpty()) ? Integer.parseInt(staffIdParam) : 0;
        staff.setStaffID(staffId);
        staff.setFullName(request.getParameter("fullName").trim());
        staff.setGender(Boolean.parseBoolean(request.getParameter("gender")));
        staff.setPhoneNumber(request.getParameter("phoneNumber").trim());
        staff.setEmail(request.getParameter("email").trim());
        staff.setIsActive(Boolean.parseBoolean(request.getParameter("isActive")));
        if ("create".equals(action)) {
            // Password is only captured during creation and is not trimmed.
            staff.setPassword(request.getParameter("password"));
        }

        Role role = new Role();
        role.setRoleID(Integer.parseInt(request.getParameter("roleID")));
        staff.setRole(role);

        // --- Step 2: Perform server-side validation for uniqueness ---
        String errorMessage = null;
        try {
            if (staffDAO.isEmailExists(staff.getEmail(), staff.getStaffID())) {
                errorMessage = "Email already exists. Please choose another one.";
            } else if (staffDAO.isFullNameExists(staff.getFullName(), staff.getStaffID())) {
                errorMessage = "Full name already exists. Please choose another one.";
            } else if (staffDAO.isPhoneNumberExists(staff.getPhoneNumber(), staff.getStaffID())) {
                errorMessage = "Phone number already exists. Please choose another one.";
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            errorMessage = "Database error during validation.";
        }

        // --- Step 3: Handle validation failure ---
        // If an error message was set, it means validation failed.
        if (errorMessage != null) {
            // Add the error message and the user-submitted data back into the request.
            request.setAttribute("errorMessage", errorMessage);
            request.setAttribute("staff", staff); // This preserves the user's input in the form fields.

            // Also, reload the list of roles for the dropdown.
            List<Role> roleList = roleDAO.getAllRoles();
            request.setAttribute("roleList", roleList);

            // Forward the request back to the form page to display the error and the preserved data.
            // Using forward is crucial here instead of redirect to maintain the request attributes.
            request.getRequestDispatcher("staff-form.jsp").forward(request, response);
            return; // Stop further processing to prevent the invalid data from being saved.
        }

        // --- Step 4: Handle validation success ---
        // If there were no errors, proceed with the database operation.
        if ("create".equals(action)) {
            staffService.createStaff(staff);
        } else if ("update".equals(action)) {
            staffDAO.updateStaff(staff);
        }

        // After a successful operation, redirect the user to the staff list page.
        // A redirect is used to prevent form resubmission issues if the user refreshes the page.
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
        StaffDAO staffDAO = new StaffDAO();
        RoleDAO roleDAO = new RoleDAO();

        if ("delete".equals(action)) {
            // Handle deletion action.
            int staffId = Integer.parseInt(request.getParameter("id"));
            staffDAO.deleteStaff(staffId);
            response.sendRedirect("staff-list");
        } else {
            // Handles both "create" and "edit" actions, as both need to display the form.
            // First, always fetch the list of roles for the dropdown.
            List<Role> roleList = roleDAO.getAllRoles();
            request.setAttribute("roleList", roleList);

            if ("edit".equals(action)) {
                // If editing, fetch the existing staff member's data to pre-populate the form.
                int staffId = Integer.parseInt(request.getParameter("id"));
                Staff staff = staffDAO.getStaffById(staffId);
                request.setAttribute("staff", staff);
            }
            // If creating, we just need the empty form with the role list.

            // Forward to the JSP form for display.
            request.getRequestDispatcher("staff-form.jsp").forward(request, response);
        }
    }
}
