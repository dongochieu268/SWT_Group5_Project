package fu.swt301.sms.filter;

import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminFilterTest {

    @Test
    public void redirectsToLoginWhenNotLoggedIn() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/StaffManagement");

        new AdminFilter().doFilter(request, response, chain);

        verify(response).sendRedirect("/StaffManagement/login");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void returns403WhenUserIsNotAdmin() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain chain = mock(FilterChain.class);
        Role role = new Role();
        role.setRoleName("Staff");
        Staff staff = new Staff();
        staff.setRole(role);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(staff);

        new AdminFilter().doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void returns403WhenRoleIsNull() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain chain = mock(FilterChain.class);
        Staff staff = new Staff();
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(staff);

        new AdminFilter().doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void allowsAdminRequest() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain chain = mock(FilterChain.class);
        Role role = new Role();
        role.setRoleName("Admin");
        Staff staff = new Staff();
        staff.setRole(role);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(staff);

        new AdminFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }
}
