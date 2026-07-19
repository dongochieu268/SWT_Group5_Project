package fu.swt301.sms.servlet;

import fu.swt301.sms.dao.RoleDAO;
import fu.swt301.sms.dao.StaffDAO;
import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.StaffNotFoundException;
import fu.swt301.sms.service.StaffService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
    public void updateWithNonExistentStaffIdReturns404() throws Exception {
        HttpServletRequest request = validUpdateRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        org.mockito.Mockito.doThrow(new StaffNotFoundException(42))
                .when(staffService).updateStaff(any(Staff.class));
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, mock(RoleDAO.class));

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Staff not found");
        verify(response, never()).sendRedirect(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    public void editWithNonExistentStaffIdReturns404() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StaffService staffService = mock(StaffService.class);
        RoleDAO roleDAO = mock(RoleDAO.class);
        when(request.getParameter("action")).thenReturn("edit");
        when(request.getParameter("id")).thenReturn("999");
        when(staffService.getStaffById(999)).thenReturn(null);
        StaffCrudServlet servlet = new StaffCrudServlet(
                mock(StaffDAO.class), staffService, roleDAO);

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Staff not found");
        verify(request, never()).getRequestDispatcher(org.mockito.ArgumentMatchers.anyString());
    }

    private HttpServletRequest validUpdateRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("action")).thenReturn("update");
        when(request.getParameter("staffID")).thenReturn("42");
        when(request.getParameter("employeeCode")).thenReturn("emp001");
        when(request.getParameter("fullName")).thenReturn("New Staff");
        when(request.getParameter("gender")).thenReturn("true");
        when(request.getParameter("dateOfBirth")).thenReturn("1995-01-01");
        when(request.getParameter("phoneNumber")).thenReturn("0987654321");
        when(request.getParameter("email")).thenReturn("NEW.STAFF@example.com");
        when(request.getParameter("department")).thenReturn("Engineering");
        when(request.getParameter("position")).thenReturn("Developer");
        when(request.getParameter("salary")).thenReturn("1000.00");
        when(request.getParameter("hireDate")).thenReturn("2026-07-18");
        when(request.getParameter("roleID")).thenReturn("2");
        when(request.getParameter("isActive")).thenReturn("true");
        return request;
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
