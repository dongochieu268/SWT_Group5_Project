<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
    <title>Staff Detail</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h2 class="text-center mt-5">Staff Detail</h2>
    <table class="table table-bordered">
        <tbody>
        <tr>
            <th>ID</th>
            <td><c:out value="${staff.staffID}"/></td>
        </tr>
        <tr>
            <th>Full Name</th>
            <td><c:out value="${staff.fullName}"/></td>
        </tr>
        <tr>
            <th>Gender</th>
            <td><c:out value="${staff.gender ? 'Male' : 'Female'}"/></td>
        </tr>
        <tr>
            <th>Phone Number</th>
            <td><c:out value="${staff.phoneNumber}"/></td>
        </tr>
        <tr>
            <th>Email</th>
            <td><c:out value="${staff.email}"/></td>
        </tr>
        <tr>
            <th>Role</th>
            <td><c:out value="${staff.role.roleName}"/></td>
        </tr>
        <tr>
            <th>Status</th>
            <td><c:out value="${staff.isActive ? 'Active' : 'Inactive'}"/></td>
        </tr>
        </tbody>
    </table>
    <a href="staff-list" class="btn btn-secondary">Back to List</a>
</div>
</body>
</html>
