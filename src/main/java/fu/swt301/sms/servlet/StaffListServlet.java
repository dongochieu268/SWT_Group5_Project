package fu.swt301.sms.servlet;

import fu.swt301.sms.utils.CsrfTokenManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import fu.swt301.sms.service.StaffPage;
import fu.swt301.sms.service.StaffService;

import java.io.IOException;

@WebServlet("/staff-list")
public class StaffListServlet extends HttpServlet {
    private final StaffService staffService;

    public StaffListServlet() {
        this(new StaffService());
    }

    StaffListServlet(StaffService staffService) {
        this.staffService = staffService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String searchKeyword = firstNonEmpty(request.getParameter("searchKeyword"), request.getParameter("searchName"));
        String searchDepartment = request.getParameter("searchDepartment");
        String searchStatus = request.getParameter("searchStatus");
        String page = request.getParameter("page");
        String pageSize = request.getParameter("pageSize");

        StaffPage staffPage = staffService.getStaffPage(searchKeyword, searchDepartment, searchStatus, page, pageSize);

        request.setAttribute("staffPage", staffPage);
        request.setAttribute("staffList", staffPage.getStaffList());
        request.setAttribute("searchKeyword", searchKeyword);
        request.setAttribute("searchDepartment", searchDepartment);
        request.setAttribute("searchStatus", searchStatus);
        CsrfTokenManager.ensureToken(request);
        request.getRequestDispatcher("staff-list.jsp").forward(request, response);
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }
}
