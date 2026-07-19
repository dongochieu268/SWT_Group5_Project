package fu.swt301.sms.servlet;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffService;
import fu.swt301.sms.service.StaffValidationException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaffCrudServletTest {

    @Test
    public void postWithoutCsrfTokenIsForbidden() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);

        servletWithMocks().doPost(mock(HttpServletRequest.class), response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    public void postWithMismatchedCsrfTokenIsForbidden() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getParameter("csrfToken")).thenReturn("submitted-token");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("expected-token");

        servletWithMocks().doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void deleteCannotBePerformedWithGet() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("action")).thenReturn("delete");

        servletWithMocks().doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void createParsesAllRequiredFieldsAndDelegatesToService() throws Exception {
        HttpServletRequest request = validCreateRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffCrudServlet servlet = new StaffCrudServlet(mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        ArgumentCaptor<Staff> staffCaptor = ArgumentCaptor.forClass(Staff.class);
        verify(staffService).createStaff(staffCaptor.capture());
        Staff staff = staffCaptor.getValue();
        assertEquals("EMP001", staff.getEmployeeCode());
        assertEquals(LocalDate.of(1995, 1, 1), staff.getDateOfBirth());
        assertEquals("Engineering", staff.getDepartment());
        assertEquals("Developer", staff.getPosition());
        assertEquals(new BigDecimal("1000.00"), staff.getSalary());
        assertEquals(LocalDate.of(2026, 7, 18), staff.getHireDate());
        verify(response).sendRedirect("staff-list");
    }

    @Test
    public void createRejectsMissingGenderAndPreservesForm() throws Exception {
        FormRequest formRequest = invalidCreateRequest("gender", null);

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.staffService, never()).createStaff(any(Staff.class));
        verify(formRequest.request).setAttribute("errorMessage", "Gender is required.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void createRejectsMissingStatusAndPreservesForm() throws Exception {
        FormRequest formRequest = invalidCreateRequest("isActive", null);

        formRequest.servlet.doPost(formRequest.request, formRequest.response);

        verify(formRequest.staffService, never()).createStaff(any(Staff.class));
        verify(formRequest.request).setAttribute("errorMessage", "Status is required.");
        verify(formRequest.dispatcher).forward(formRequest.request, formRequest.response);
    }

    @Test
    public void deleteDelegatesToServiceWithCurrentUserAndRedirects() throws Exception {
        HttpServletRequest request = validRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        when(request.getParameter("action")).thenReturn("delete");
        when(request.getParameter("id")).thenReturn("42");
        withCurrentUser(request, 1);
        StaffCrudServlet servlet = new StaffCrudServlet(mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        verify(staffService).deleteStaff(42, 1);
        verify(response).sendRedirect("staff-list");
    }

    @Test
    public void deleteRejectsSelfDeleteWithBadRequest() throws Exception {
        HttpServletRequest request = validRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        when(request.getParameter("action")).thenReturn("delete");
        when(request.getParameter("id")).thenReturn("1");
        withCurrentUser(request, 1);
        doThrow(new StaffValidationException("You cannot delete your own account while logged in."))
                .when(staffService).deleteStaff(1, 1);
        StaffCrudServlet servlet = new StaffCrudServlet(mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                "You cannot delete your own account while logged in.");
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    public void deleteRejectsInvalidIdWithoutCallingService() throws Exception {
        HttpServletRequest request = validRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        when(request.getParameter("action")).thenReturn("delete");
        when(request.getParameter("id")).thenReturn("not-a-number");
        StaffCrudServlet servlet = new StaffCrudServlet(mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        verify(staffService, never()).deleteStaff(anyInt(), anyInt());
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    private StaffCrudServlet servletWithMocks() {
        RoleDAO roleDAO = mock(RoleDAO.class);
        when(roleDAO.getAllRoles()).thenReturn(Collections.emptyList());
        return new StaffCrudServlet(mock(StaffDAO.class), mock(StaffService.class), roleDAO);
    }

    private FormRequest invalidCreateRequest(String parameterName, String invalidValue) {
        StaffService staffService = mock(StaffService.class);
        RoleDAO roleDAO = mock(RoleDAO.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        HttpServletRequest request = validCreateRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter(parameterName)).thenReturn(invalidValue);
        when(roleDAO.getAllRoles()).thenReturn(Collections.emptyList());
        when(request.getRequestDispatcher("staff-form.jsp")).thenReturn(dispatcher);
        return new FormRequest(new StaffCrudServlet(mock(StaffDAO.class), staffService, roleDAO),
                staffService, request, response, dispatcher);
    }

    private HttpServletRequest validCreateRequest() {
        HttpServletRequest request = validRequest();
        when(request.getParameter("action")).thenReturn("create");
        when(request.getParameter("employeeCode")).thenReturn("emp001");
        when(request.getParameter("fullName")).thenReturn("New Staff");
        when(request.getParameter("gender")).thenReturn("true");
        when(request.getParameter("dateOfBirth")).thenReturn("1995-01-01");
        when(request.getParameter("phoneNumber")).thenReturn("0987654321");
        when(request.getParameter("email")).thenReturn("NEW.STAFF@example.com");
        when(request.getParameter("password")).thenReturn("staff123");
        when(request.getParameter("department")).thenReturn("Engineering");
        when(request.getParameter("position")).thenReturn("Developer");
        when(request.getParameter("salary")).thenReturn("1000.00");
        when(request.getParameter("hireDate")).thenReturn("2026-07-18");
        when(request.getParameter("roleID")).thenReturn("2");
        when(request.getParameter("isActive")).thenReturn("true");
        return request;
    }

    private HttpServletRequest validRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getParameter("csrfToken")).thenReturn("valid-token");
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("csrfToken")).thenReturn("valid-token");
        return request;
    }

    private void withCurrentUser(HttpServletRequest request, int staffId) {
        Staff currentUser = new Staff();
        currentUser.setStaffID(staffId);
        when(request.getSession(false).getAttribute("user")).thenReturn(currentUser);
    }

    private static class FormRequest {
        private final StaffCrudServlet servlet;
        private final StaffService staffService;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final RequestDispatcher dispatcher;

        private FormRequest(StaffCrudServlet servlet, StaffService staffService,
                HttpServletRequest request, HttpServletResponse response, RequestDispatcher dispatcher) {
            this.servlet = servlet;
            this.staffService = staffService;
            this.request = request;
            this.response = response;
            this.dispatcher = dispatcher;
        }
    }
}
