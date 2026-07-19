package fu.swt301.sms.servlet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fu.swt301.sms.entity.Role;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.filter.AuthFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the real interaction between {@link AuthFilter} and
 * {@link LogoutServlet} around session lifecycle (FR-04 + FR-05 boundary).
 *
 * <p>Unlike {@link LogoutServletTest}, this uses a spec-compliant fake
 * {@link HttpSession} instead of a Mockito mock, so {@code invalidate()} has
 * real effect: attribute access after invalidation throws
 * {@link IllegalStateException}, exactly like a real servlet container.
 */
public class LogoutFilterChainIT {

    @Test
    public void loggedInSessionPassesAuthFilterBeforeLogoutAndIsRejectedAfter() throws Exception {
        FakeHttpSession session = new FakeHttpSession();
        Staff staff = new Staff();
        Role role = new Role();
        role.setRoleName("Staff");
        staff.setRole(role);
        session.setAttribute("user", staff);

        HttpServletRequest beforeLogoutRequest = mock(HttpServletRequest.class);
        HttpServletResponse beforeLogoutResponse = mock(HttpServletResponse.class);
        FilterChain beforeLogoutChain = mock(FilterChain.class);
        when(beforeLogoutRequest.getSession(false)).thenReturn(session);

        new AuthFilter().doFilter(beforeLogoutRequest, beforeLogoutResponse, beforeLogoutChain);

        verify(beforeLogoutChain).doFilter(beforeLogoutRequest, beforeLogoutResponse);

        HttpServletRequest logoutRequest = mock(HttpServletRequest.class);
        HttpServletResponse logoutResponse = mock(HttpServletResponse.class);
        when(logoutRequest.getSession(false)).thenReturn(session);
        when(logoutRequest.getContextPath()).thenReturn("/StaffManagement");

        new LogoutServlet().doPost(logoutRequest, logoutResponse);

        HttpServletRequest afterLogoutRequest = mock(HttpServletRequest.class);
        HttpServletResponse afterLogoutResponse = mock(HttpServletResponse.class);
        FilterChain afterLogoutChain = mock(FilterChain.class);
        when(afterLogoutRequest.getSession(false)).thenReturn(null);
        when(afterLogoutRequest.getContextPath()).thenReturn("/StaffManagement");

        new AuthFilter().doFilter(afterLogoutRequest, afterLogoutResponse, afterLogoutChain);

        verify(afterLogoutResponse).sendRedirect("/StaffManagement/login");
        verify(afterLogoutChain, never()).doFilter(afterLogoutRequest, afterLogoutResponse);
    }

    @Test
    public void logoutInvalidatesTheRealSessionObject() throws Exception {
        FakeHttpSession session = new FakeHttpSession();
        session.setAttribute("user", new Staff());

        HttpServletRequest logoutRequest = mock(HttpServletRequest.class);
        HttpServletResponse logoutResponse = mock(HttpServletResponse.class);
        when(logoutRequest.getSession(false)).thenReturn(session);
        when(logoutRequest.getContextPath()).thenReturn("/StaffManagement");

        new LogoutServlet().doPost(logoutRequest, logoutResponse);

        assertTrue(session.isInvalidated());
        assertThrows(IllegalStateException.class, () -> session.getAttribute("user"));
    }

    /**
     * Minimal spec-compliant {@link HttpSession}: after {@code invalidate()},
     * attribute access throws {@link IllegalStateException} like a real
     * container, instead of silently succeeding like a Mockito mock would.
     */
    private static final class FakeHttpSession implements HttpSession {
        private final Map<String, Object> attributes = new HashMap<>();
        private final long creationTime = System.currentTimeMillis();
        private boolean invalidated;
        private int maxInactiveInterval;

        boolean isInvalidated() {
            return invalidated;
        }

        private void checkValid() {
            if (invalidated) {
                throw new IllegalStateException("Session already invalidated");
            }
        }

        @Override
        public long getCreationTime() {
            checkValid();
            return creationTime;
        }

        @Override
        public String getId() {
            return "fake-session-id";
        }

        @Override
        public long getLastAccessedTime() {
            checkValid();
            return creationTime;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public void setMaxInactiveInterval(int interval) {
            this.maxInactiveInterval = interval;
        }

        @Override
        public int getMaxInactiveInterval() {
            return maxInactiveInterval;
        }

        @Override
        public HttpSessionContext getSessionContext() {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            checkValid();
            return attributes.get(name);
        }

        @Override
        public Object getValue(String name) {
            return getAttribute(name);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            checkValid();
            return Collections.enumeration(attributes.keySet());
        }

        @Override
        public String[] getValueNames() {
            checkValid();
            return attributes.keySet().toArray(new String[0]);
        }

        @Override
        public void setAttribute(String name, Object value) {
            checkValid();
            attributes.put(name, value);
        }

        @Override
        public void putValue(String name, Object value) {
            setAttribute(name, value);
        }

        @Override
        public void removeAttribute(String name) {
            checkValid();
            attributes.remove(name);
        }

        @Override
        public void removeValue(String name) {
            removeAttribute(name);
        }

        @Override
        public void invalidate() {
            checkValid();
            invalidated = true;
            attributes.clear();
        }

        @Override
        public boolean isNew() {
            checkValid();
            return false;
        }
    }
}
