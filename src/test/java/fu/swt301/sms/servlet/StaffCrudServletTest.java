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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaffCrudServletTest {
    @Test
    public void createParsesAllRequiredFieldsAndDelegatesToService() throws Exception {
        HttpServletRequest request = validCreateRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, mock(RoleDAO.class));

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
    public void createRejectsMissingGenderOnServerAndPreservesForm() throws Exception {
        HttpServletRequest request = validCreateRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        RoleDAO roleDAO = mock(RoleDAO.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("gender")).thenReturn(null);
        when(roleDAO.getAllRoles()).thenReturn(Collections.emptyList());
        when(request.getRequestDispatcher("staff-form.jsp")).thenReturn(dispatcher);
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, roleDAO);

        servlet.doPost(request, response);

        verify(staffService, never()).createStaff(any(Staff.class));
        verify(request).setAttribute("errorMessage", "Gender is required.");
        verify(request).setAttribute(org.mockito.ArgumentMatchers.eq("staff"), any(Staff.class));
        verify(dispatcher).forward(request, response);
    }

    @Test
    public void createRejectsMissingStatusOnServer() throws Exception {
        HttpServletRequest request = validCreateRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        RoleDAO roleDAO = mock(RoleDAO.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("isActive")).thenReturn(null);
        when(roleDAO.getAllRoles()).thenReturn(Collections.emptyList());
        when(request.getRequestDispatcher("staff-form.jsp")).thenReturn(dispatcher);
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, roleDAO);

        servlet.doPost(request, response);

        verify(staffService, never()).createStaff(any(Staff.class));
        verify(request).setAttribute("errorMessage", "Status is required.");
        verify(dispatcher).forward(request, response);
    }

    @Test
    public void deleteDelegatesToServiceWithCurrentUserIdAndRedirects() throws Exception {
        HttpServletRequest request = deleteRequest("42");
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        withCurrentUser(request, 1);
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        verify(staffService).deleteStaff(42, 1);
        verify(response).sendRedirect("staff-list");
    }

    @Test
    public void deleteRejectsSelfDeleteWithBadRequest() throws Exception {
        HttpServletRequest request = deleteRequest("1");
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        withCurrentUser(request, 1);
        doThrow(new StaffValidationException("You cannot delete your own account while logged in."))
                .when(staffService).deleteStaff(1, 1);
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
                eq("You cannot delete your own account while logged in."));
        verify(response, never()).sendRedirect(any());
    }

    @Test
    public void deleteRejectsInvalidIdWithoutCallingService() throws Exception {
        HttpServletRequest request = deleteRequest("not-a-number");
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        verify(staffService, never()).deleteStaff(anyInt(), anyInt());
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
    }

    private HttpServletRequest deleteRequest(String id) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("action")).thenReturn("delete");
        when(request.getParameter("id")).thenReturn(id);
        return request;
    }

    private void withCurrentUser(HttpServletRequest request, int staffId) {
        Staff currentUser = new Staff();
        currentUser.setStaffID(staffId);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn(currentUser);
        when(request.getSession(false)).thenReturn(session);
    }

    private HttpServletRequest validCreateRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
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
}
