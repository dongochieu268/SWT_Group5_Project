package fu.swt301.sms.servlet;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.service.StaffService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaffCrudServletTest {

    @Test
    public void postWithoutValidCsrfTokenIsForbidden() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        new StaffCrudServlet().doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    public void deleteCannotBePerformedWithGet() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("action")).thenReturn("delete");
        when(request.getParameter("id")).thenReturn("1");

        new StaffCrudServlet().doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    public void postWithMismatchedCsrfTokenIsForbidden() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getParameter("csrfToken")).thenReturn("submitted-token");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("session-token");

        new StaffCrudServlet().doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    public void deleteWithInvalidIdIsBadRequest() throws Exception {
        StaffCrudServlet servlet = servletWithMockDependencies();
        HttpServletRequest request = validCsrfRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("action")).thenReturn("delete");
        when(request.getParameter("id")).thenReturn("-1");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    public void unsupportedPostActionIsBadRequest() throws Exception {
        StaffCrudServlet servlet = servletWithMockDependencies();
        HttpServletRequest request = validCsrfRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("action")).thenReturn("archive");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void invalidEmailReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("email", "invalid-email");

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Email format is invalid.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
        verify(formRequest.response, never()).sendRedirect(anyString());
    }

    @Test
    public void invalidPhoneNumberReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("phoneNumber", "123456789");

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Phone number must be 10 digits and start with 0.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void missingRequiredFullNameReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("fullName", " ");

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Full name is required.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void missingGenderReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("gender", null);

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Gender is required.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void missingStatusReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("isActive", null);

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Status is required.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void missingRoleReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("roleID", "0");

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Role is required.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void createWithOverlongPasswordReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("password", repeat("a", 51));

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Password must be 50 characters or fewer.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void createWithoutPasswordReturnsToFormWithError() throws Exception {
        FormRequest formRequest = invalidCreateRequest("password", null);

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.request).setAttribute("errorMessage", "Password is required.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    private StaffCrudServlet servletWithMockDependencies() {
        StaffDAO staffDAO = mock(StaffDAO.class);
        StaffService staffService = mock(StaffService.class);
        RoleDAO roleDAO = mock(RoleDAO.class);
        when(roleDAO.getAllRoles()).thenReturn(Collections.emptyList());
        return new StaffCrudServlet(staffDAO, staffService, roleDAO);
    }

    private HttpServletRequest validCsrfRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getParameter("csrfToken")).thenReturn("valid-token");
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        return request;
    }

    private FormRequest invalidCreateRequest(String parameterName, String invalidValue) {
        StaffCrudServlet servlet = servletWithMockDependencies();
        HttpServletRequest request = validCsrfRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("action")).thenReturn("create");
        when(request.getParameter("fullName")).thenReturn("Test User");
        when(request.getParameter("gender")).thenReturn("true");
        when(request.getParameter("phoneNumber")).thenReturn("0912345678");
        when(request.getParameter("email")).thenReturn("test@example.com");
        when(request.getParameter("isActive")).thenReturn("true");
        when(request.getParameter("roleID")).thenReturn("1");
        when(request.getParameter("password")).thenReturn("password");
        when(request.getParameter(parameterName)).thenReturn(invalidValue);
        when(request.getRequestDispatcher("staff-form.jsp")).thenReturn(dispatcher);
        return new FormRequest(servlet, request, response, dispatcher);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private static class FormRequest {
        private final StaffCrudServlet servlet;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final RequestDispatcher dispatcher;

        private FormRequest(StaffCrudServlet servlet, HttpServletRequest request,
                HttpServletResponse response, RequestDispatcher dispatcher) {
            this.servlet = servlet;
            this.request = request;
            this.response = response;
            this.dispatcher = dispatcher;
        }
    }
}
