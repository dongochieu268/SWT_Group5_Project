package fu.swt301.sms.servlet;

import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaffDetailServletTest {
    @Test
    public void returns400WhenIdMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        when(request.getParameter("id")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    @Test
    public void returns400WhenIdIsNotANumber() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        when(request.getParameter("id")).thenReturn("abc");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    @Test
    public void returns400WhenIdIsLessThanOne() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        when(request.getParameter("id")).thenReturn("0");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    @Test
    public void returns404WhenStaffNotFound() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        when(request.getParameter("id")).thenReturn("1");
        when(staffService.getStaffById(1)).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    @Test
    public void forwardsToDetailPageWhenStaffFound() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        Staff staff = new Staff();
        staff.setStaffID(1);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("id")).thenReturn("1");
        when(staffService.getStaffById(1)).thenReturn(staff);
        when(request.getRequestDispatcher("staff-detail.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("staff", staff);
        verify(dispatcher).forward(request, response);
        verify(response, never()).sendError(anyInt());
    }
}
