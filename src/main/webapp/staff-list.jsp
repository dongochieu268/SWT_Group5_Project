<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>Staff List</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h2 class="text-center mt-5">Staff Management</h2>
    <div class="row mb-3">
        <div class="col-md-9">
            <form class="form-inline" action="staff-list" method="get">
                <div class="form-group mr-2 mb-2">
                    <input type="text" class="form-control" name="searchKeyword" placeholder="Name or employee code" value="<c:out value='${searchKeyword}'/>">
                </div>
                <div class="form-group mr-2 mb-2">
                    <input type="text" class="form-control" name="searchDepartment" placeholder="Department" value="<c:out value='${searchDepartment}'/>">
                </div>
                <div class="form-group mr-2 mb-2">
                    <select class="form-control" name="searchStatus">
                        <option value="">All Statuses</option>
                        <option value="true" ${searchStatus == 'true' ? 'selected' : ''}>Active</option>
                        <option value="false" ${searchStatus == 'false' ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
                <div class="form-group mr-2 mb-2">
                    <select class="form-control" name="pageSize">
                        <option value="10" ${staffPage.pageSize == 10 ? 'selected' : ''}>10/page</option>
                        <option value="20" ${staffPage.pageSize == 20 ? 'selected' : ''}>20/page</option>
                        <option value="50" ${staffPage.pageSize == 50 ? 'selected' : ''}>50/page</option>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary mb-2">Filter</button>
            </form>
        </div>
        <div class="col-md-3 text-right">
            <a href="staff-crud?action=create" class="btn btn-success">Add New Staff</a>
        </div>
    </div>
    <div class="mb-2 text-muted">
        Showing page <c:out value="${staffPage.currentPage}"/> of
        <c:out value="${staffPage.totalPages == 0 ? 1 : staffPage.totalPages}"/>,
        total <c:out value="${staffPage.totalItems}"/> staff.
    </div>
    <table class="table table-bordered">
        <thead>
        <tr>
            <th>ID</th>
            <th>Employee Code</th>
            <th>Full Name</th>
            <th>Department</th>
            <th>Gender</th>
            <th>Phone Number</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="staff" items="${staffList}">
            <tr>
                <td><c:out value="${staff.staffID}"/></td>
                <td><c:out value="${staff.employeeCode}"/></td>
                <td><c:out value="${staff.fullName}"/></td>
                <td><c:out value="${staff.department}"/></td>
                <td>${staff.gender ? 'Male' : 'Female'}</td>
                <td><c:out value="${staff.phoneNumber}"/></td>
                <td><c:out value="${staff.email}"/></td>
                <td><c:out value="${staff.role.roleName}"/></td>
                <td>${staff.isActive ? 'Active' : 'Inactive'}</td>
                <td>
                    <a href="staff-detail?id=${staff.staffID}" class="btn btn-sm btn-info">View</a>
                    <a href="staff-crud?action=edit&id=${staff.staffID}" class="btn btn-sm btn-warning">Edit</a>
                    <form action="staff-crud" method="post" class="d-inline" onsubmit="return confirm('Are you sure?')">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="id" value="${staff.staffID}">
                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                        <button type="submit" class="btn btn-sm btn-danger">Delete</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty staffList}">
            <tr>
                <td colspan="10" class="text-center text-muted">No staff found.</td>
            </tr>
        </c:if>
        </tbody>
    </table>
    <c:if test="${staffPage.totalPages > 1}">
        <nav aria-label="Staff pagination">
            <ul class="pagination justify-content-center">
                <li class="page-item ${staffPage.hasPrevious ? '' : 'disabled'}">
                    <c:url var="previousUrl" value="staff-list">
                        <c:param name="searchKeyword" value="${searchKeyword}"/>
                        <c:param name="searchDepartment" value="${searchDepartment}"/>
                        <c:param name="searchStatus" value="${searchStatus}"/>
                        <c:param name="pageSize" value="${staffPage.pageSize}"/>
                        <c:param name="page" value="${staffPage.currentPage - 1}"/>
                    </c:url>
                    <a class="page-link" href="${previousUrl}">Previous</a>
                </li>
                <c:forEach var="pageNumber" begin="1" end="${staffPage.totalPages}">
                    <c:url var="pageUrl" value="staff-list">
                        <c:param name="searchKeyword" value="${searchKeyword}"/>
                        <c:param name="searchDepartment" value="${searchDepartment}"/>
                        <c:param name="searchStatus" value="${searchStatus}"/>
                        <c:param name="pageSize" value="${staffPage.pageSize}"/>
                        <c:param name="page" value="${pageNumber}"/>
                    </c:url>
                    <li class="page-item ${pageNumber == staffPage.currentPage ? 'active' : ''}">
                        <a class="page-link" href="${pageUrl}"><c:out value="${pageNumber}"/></a>
                    </li>
                </c:forEach>
                <li class="page-item ${staffPage.hasNext ? '' : 'disabled'}">
                    <c:url var="nextUrl" value="staff-list">
                        <c:param name="searchKeyword" value="${searchKeyword}"/>
                        <c:param name="searchDepartment" value="${searchDepartment}"/>
                        <c:param name="searchStatus" value="${searchStatus}"/>
                        <c:param name="pageSize" value="${staffPage.pageSize}"/>
                        <c:param name="page" value="${staffPage.currentPage + 1}"/>
                    </c:url>
                    <a class="page-link" href="${nextUrl}">Next</a>
                </li>
            </ul>
        </nav>
    </c:if>
</div>
</body>
</html>
