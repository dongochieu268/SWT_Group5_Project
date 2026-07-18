package fu.swt301.sms.servlet;

import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaffListServletTest {
    @Test
    public void usesInjectedDaoAndForwardsResults() throws Exception {
        StaffDAO staffDAO = mock(StaffDAO.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        List<Staff> expected = Collections.singletonList(new Staff());

        when(request.getParameter("searchName")).thenReturn("Minh");
        when(request.getParameter("searchStatus")).thenReturn("true");
        when(staffDAO.getStaffByFilter("Minh", "true")).thenReturn(expected);
        when(request.getRequestDispatcher("staff-list.jsp")).thenReturn(dispatcher);

        new StaffListServlet(staffDAO).doGet(request, response);

        verify(staffDAO).getStaffByFilter("Minh", "true");
        verify(request).setAttribute("staffList", expected);
        verify(dispatcher).forward(request, response);
    }
}
