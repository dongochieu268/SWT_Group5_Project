package fu.swt301.sms.servlet;

import fu.swt301.sms.service.AuthService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginServletTest {
    private static final String SAFE_ERROR = "Invalid email or password";

    @Test
    public void missingAccountUsesSafeGenericMessage() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.authenticate("missing@example.com", "secret")).thenReturn(null);

        LoginRequest request = loginRequest("missing@example.com", "secret");
        new LoginServlet(authService).doPost(request.request, request.response);

        verify(request.request).setAttribute("error", SAFE_ERROR);
        verify(request.dispatcher).forward(request.request, request.response);
        verify(request.response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    public void wrongPasswordUsesSameSafeGenericMessage() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.authenticate("user@example.com", "wrong")).thenReturn(null);

        LoginRequest request = loginRequest("user@example.com", "wrong");
        new LoginServlet(authService).doPost(request.request, request.response);

        verify(request.request).setAttribute("error", SAFE_ERROR);
        verify(request.dispatcher).forward(request.request, request.response);
    }

    @Test
    public void internalFailureDoesNotExposeExceptionDetails() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.authenticate("user@example.com", "secret"))
                .thenThrow(new IllegalStateException("jdbc:sqlserver://secret-host; password=secret"));

        LoginRequest request = loginRequest("user@example.com", "secret");
        new LoginServlet(authService).doPost(request.request, request.response);

        verify(request.request).setAttribute("error", SAFE_ERROR);
        verify(request.dispatcher).forward(request.request, request.response);
    }

    private LoginRequest loginRequest(String email, String password) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("email")).thenReturn(email);
        when(request.getParameter("password")).thenReturn(password);
        when(request.getRequestDispatcher("login.jsp")).thenReturn(dispatcher);
        return new LoginRequest(request, response, dispatcher);
    }

    private static class LoginRequest {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final RequestDispatcher dispatcher;

        private LoginRequest(HttpServletRequest request, HttpServletResponse response, RequestDispatcher dispatcher) {
            this.request = request;
            this.response = response;
            this.dispatcher = dispatcher;
        }
    }
}
