package fu.swt301.sms.servlet;

import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;

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
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("id")).thenReturn(null);
        when(request.getRequestDispatcher("error-400.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(dispatcher).forward(request, response);
    }

    @Test
    public void returns400WhenIdIsNotANumber() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("id")).thenReturn("abc");
        when(request.getRequestDispatcher("error-400.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(dispatcher).forward(request, response);
    }

    @Test
    public void returns400WhenIdIsLessThanOne() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("id")).thenReturn("0");
        when(request.getRequestDispatcher("error-400.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(dispatcher).forward(request, response);
    }

    @Test
    public void returns404WhenStaffNotFound() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffDetailServlet servlet = new StaffDetailServlet(staffService);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("id")).thenReturn("1");
        when(staffService.getStaffById(1)).thenReturn(null);
        when(request.getRequestDispatcher("error-404.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(dispatcher).forward(request, response);
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
        verify(response, never()).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
