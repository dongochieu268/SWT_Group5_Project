package fu.swt301.sms.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CsrfTokenManagerTest {

    @Test
    public void ensureTokenStoresNewTokenInSessionAndRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        String token = CsrfTokenManager.ensureToken(request);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(session).setAttribute(eq("csrfToken"), tokenCaptor.capture());
        assertEquals(tokenCaptor.getValue(), token);
        verify(request).setAttribute("csrfToken", token);
    }

    @Test
    public void ensureTokenReusesExistingSessionToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("existing-token");

        String token = CsrfTokenManager.ensureToken(request);

        assertEquals("existing-token", token);
        verify(session, never()).setAttribute(eq("csrfToken"), org.mockito.ArgumentMatchers.anyString());
        verify(request).setAttribute("csrfToken", "existing-token");
    }

    @Test
    public void validTokenMatchesSessionToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getParameter("csrfToken")).thenReturn("token-123");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("token-123");

        assertTrue(CsrfTokenManager.isValid(request));
    }

    @Test
    public void missingOrMismatchedTokenIsRejected() {
        HttpServletRequest missingTokenRequest = mock(HttpServletRequest.class);
        assertFalse(CsrfTokenManager.isValid(missingTokenRequest));

        HttpServletRequest mismatchedRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(mismatchedRequest.getParameter("csrfToken")).thenReturn("submitted");
        when(mismatchedRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("expected");

        assertFalse(CsrfTokenManager.isValid(mismatchedRequest));
    }
}
