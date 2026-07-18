package fu.swt301.sms.servlet;

import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/staff-detail")
public class StaffDetailServlet extends HttpServlet {
    private final StaffService staffService;

    public StaffDetailServlet() {
        this(new StaffService());
    }

    StaffDetailServlet(StaffService staffService) {
        this.staffService = staffService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idParam = request.getParameter("id");
        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (id < 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Staff staff = staffService.getStaffById(id);
        if (staff == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("staff", staff);
        request.getRequestDispatcher("staff-detail.jsp").forward(request, response);
    }
}
