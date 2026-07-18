package fu.swt301.sms.servlet;

import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffPage;
import fu.swt301.sms.service.StaffService;
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
    public void usesInjectedServiceAndForwardsResults() throws Exception {
        StaffService staffService = mock(StaffService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        List<Staff> expected = Collections.singletonList(new Staff());
        StaffPage expectedPage = new StaffPage(expected, 1, 10, 1);

        when(request.getParameter("searchName")).thenReturn("Minh");
        when(request.getParameter("searchStatus")).thenReturn("true");
        when(staffService.getStaffPage("Minh", null, "true", null, null)).thenReturn(expectedPage);
        when(request.getRequestDispatcher("staff-list.jsp")).thenReturn(dispatcher);

        new StaffListServlet(staffService).doGet(request, response);

        verify(staffService).getStaffPage("Minh", null, "true", null, null);
        verify(request).setAttribute("staffPage", expectedPage);
        verify(request).setAttribute("staffList", expected);
        verify(dispatcher).forward(request, response);
    }
}
