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
        <div class="col-md-6">
            <form class="form-inline" action="staff-list" method="get">
                <div class="form-group mr-2">
                    <input type="text" class="form-control" name="searchName" placeholder="Search by name" value="${param.searchName}">
                </div>
                <div class="form-group mr-2">
                    <select class="form-control" name="searchStatus">
                        <option value="">All Statuses</option>
                        <option value="true" ${param.searchStatus == 'true' ? 'selected' : ''}>Active</option>
                        <option value="false" ${param.searchStatus == 'false' ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary">Filter</button>
            </form>
        </div>
        <div class="col-md-6 text-right">
            <a href="staff-crud?action=create" class="btn btn-success">Add New Staff</a>
        </div>
    </div>
    <table class="table table-bordered">
        <thead>
        <tr>
            <th>ID</th>
            <th>Full Name</th>
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
                <td>${staff.staffID}</td>
                <td>${staff.fullName}</td>
                <td>${staff.gender ? 'Male' : 'Female'}</td>
                <td>${staff.phoneNumber}</td>
                <td>${staff.email}</td>
                <td>${staff.role.roleName}</td>
                <td>${staff.isActive ? 'Active' : 'Inactive'}</td>
                <td>
                    <a href="staff-crud?action=edit&id=${staff.staffID}" class="btn btn-sm btn-warning">Edit</a>
                    <a href="staff-crud?action=delete&id=${staff.staffID}" class="btn btn-sm btn-danger" onclick="return confirm('Are you sure?')">Delete</a>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
</body>
</html>
