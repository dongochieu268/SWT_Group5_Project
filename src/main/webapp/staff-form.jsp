<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<html>
<head>
    <title>${empty staff || staff.staffID == 0 ? 'Add Staff' : 'Edit Staff'}</title>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h2 class="text-center mt-5">${empty staff || staff.staffID == 0 ? 'Add New Staff' : 'Edit Staff'}</h2>

    <!-- Error Message Display -->
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger text-center" role="alert">
            <c:out value="${errorMessage}"/>
        </div>
    </c:if>

    <form action="staff-crud" method="post">
        <input type="hidden" name="action" value="${empty staff || staff.staffID == 0 ? 'create' : 'update'}">
        <c:if test="${not empty staff && staff.staffID != 0}">
            <input type="hidden" name="staffID" value="${staff.staffID}">
        </c:if>
        <div class="form-group">
            <label for="employeeCode">Employee Code</label>
            <input type="text" class="form-control" id="employeeCode" name="employeeCode"
                   value="${fn:escapeXml(staff.employeeCode)}"
                   required maxlength="20" pattern="[A-Z][A-Z0-9]{2,19}"
                   oninput="this.value = this.value.toUpperCase()"
                   title="Use 3-20 uppercase letters or digits, starting with a letter.">
        </div>
        <div class="form-group">
            <label for="fullName">Full Name</label>
            <input type="text" class="form-control" id="fullName" name="fullName"
                   value="${fn:escapeXml(staff.fullName)}" required maxlength="100">
        </div>
        <div class="form-group">
            <label>Gender</label><br>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="gender" id="male" value="true"
                       ${not empty staff && staff.gender ? 'checked' : ''} required>
                <label class="form-check-label" for="male">Male</label>
            </div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="gender" id="female" value="false"
                       ${not empty staff && !staff.gender ? 'checked' : ''}>
                <label class="form-check-label" for="female">Female</label>
            </div>
        </div>
        <div class="form-group">
            <label for="dateOfBirth">Date of Birth</label>
            <input type="date" class="form-control" id="dateOfBirth" name="dateOfBirth"
                   value="${staff.dateOfBirth}" max="${today}" required>
        </div>
        <div class="form-group">
            <label for="phoneNumber">Phone Number</label>
            <input type="text" class="form-control" id="phoneNumber" name="phoneNumber"
                   value="${fn:escapeXml(staff.phoneNumber)}"
                   required
                   maxlength="10"
                   pattern="0[0-9]{9}"
                   title="Phone number must be 10 digits and start with 0.">
        </div>
        <div class="form-group">
            <label for="email">Email</label>
            <input type="email" class="form-control" id="email" name="email"
                   value="${fn:escapeXml(staff.email)}" required maxlength="100">
        </div>
        <c:if test="${empty staff || staff.staffID == 0}">
            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" class="form-control" id="password" name="password" required maxlength="72">
            </div>
        </c:if>
        <div class="form-group">
            <label for="department">Department</label>
            <input type="text" class="form-control" id="department" name="department"
                   value="${fn:escapeXml(staff.department)}" required maxlength="100">
        </div>
        <div class="form-group">
            <label for="position">Position</label>
            <input type="text" class="form-control" id="position" name="position"
                   value="${fn:escapeXml(staff.position)}" required maxlength="100">
        </div>
        <div class="form-group">
            <label for="salary">Salary</label>
            <input type="number" class="form-control" id="salary" name="salary"
                   value="${staff.salary}" required min="0" step="0.01">
        </div>
        <div class="form-group">
            <label for="hireDate">Hire Date</label>
            <input type="date" class="form-control" id="hireDate" name="hireDate"
                   value="${staff.hireDate}" required>
        </div>
        <div class="form-group">
            <label for="roleID">Role</label>
            <select class="form-control" id="roleID" name="roleID" required>
                <c:forEach var="role" items="${roleList}">
                    <option value="${role.roleID}" ${staff.role.roleID == role.roleID ? 'selected' : ''}>
                        <c:out value="${role.roleName}"/>
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label>Status</label><br>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="isActive" id="active" value="true"
                       ${not empty staff && staff.isActive ? 'checked' : ''} required>
                <label class="form-check-label" for="active">Active</label>
            </div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="isActive" id="inactive" value="false"
                       ${not empty staff && !staff.isActive ? 'checked' : ''}>
                <label class="form-check-label" for="inactive">Inactive</label>
            </div>
        </div>
        <button type="submit" class="btn btn-primary">${empty staff || staff.staffID == 0 ? 'Create' : 'Update'}</button>
        <a href="staff-list" class="btn btn-secondary">Cancel</a>
    </form>
</div>
</body>
</html>
