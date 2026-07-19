package fu.swt301.sms.filter;

import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test for FR-05: exercises AuthFilter and AdminFilter composed
 * together as the real filter chain that protects /staff-crud (see web.xml),
 * instead of testing each filter in isolation with a generic mocked chain.
 */
public class AuthorizationFilterChainIT {

    private FilterChain chainInto(FilterChain targetResource) {
        return (req, resp) -> new AdminFilter().doFilter(req, resp, targetResource);
    }

    @Test
    public void anonymousUser_isRedirectedByAuthFilter_beforeReachingAdminFilterOrTarget() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain targetResource = mock(FilterChain.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/StaffManagement");

        new AuthFilter().doFilter(request, response, chainInto(targetResource));

        verify(response).sendRedirect("/StaffManagement/login");
        verify(response, never()).sendError(anyInt());
        verify(targetResource, never()).doFilter(request, response);
    }

    @Test
    public void loggedInNonAdmin_passesAuthFilterButIsBlockedByAdminFilter() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain targetResource = mock(FilterChain.class);

        Role staffRole = new Role();
        staffRole.setRoleName("Staff");
        Staff staff = new Staff();
        staff.setRole(staffRole);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(staff);

        new AuthFilter().doFilter(request, response, chainInto(targetResource));

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).sendRedirect(anyString());
        verify(targetResource, never()).doFilter(request, response);
    }

    @Test
    public void loggedInAdmin_passesThroughFullChainToTargetResource() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain targetResource = mock(FilterChain.class);

        Role adminRole = new Role();
        adminRole.setRoleName("Admin");
        Staff admin = new Staff();
        admin.setRole(adminRole);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(admin);

        new AuthFilter().doFilter(request, response, chainInto(targetResource));

        verify(targetResource).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendRedirect(anyString());
    }
}
