package fu.swt301.sms.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link LogoutServlet}.
 */
public class LogoutServletTest {

    @Test
    public void invalidatesSessionWhenSessionExists() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(request.getContextPath()).thenReturn("/StaffManagement");

        new LogoutServlet().doPost(request, response);

        verify(session).invalidate();
        verify(response).sendRedirect("/StaffManagement/login");
    }

    @Test
    public void doesNotFailWhenSessionIsNull() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/StaffManagement");

        new LogoutServlet().doPost(request, response);

        verify(response).sendRedirect("/StaffManagement/login");
    }

    @Test
    public void clearsSessionCookieWithContextPath() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/StaffManagement");

        new LogoutServlet().doPost(request, response);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertEquals("JSESSIONID", cookie.getName());
        assertEquals("/StaffManagement", cookie.getPath());
        assertEquals(0, cookie.getMaxAge());
    }

    @Test
    public void logoutTwiceDoesNotThrow() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session, (HttpSession) null);
        when(request.getContextPath()).thenReturn("/StaffManagement");

        LogoutServlet servlet = new LogoutServlet();
        servlet.doPost(request, response);
        servlet.doPost(request, response);

        verify(session).invalidate();
        verify(response, never()).sendError(org.mockito.ArgumentMatchers.anyInt());
    }
}
