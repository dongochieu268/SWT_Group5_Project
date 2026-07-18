package fu.swt301.sms.servlet;

import fu.swt301.sms.entity.Staff;
import fu.swt301.sms.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final AuthService authService;

    public LoginServlet() {
        this(new AuthService());
    }

    LoginServlet(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        Staff staff = null;
        try {
            staff = authService.authenticate(email, password);
        } catch (RuntimeException ex) {
            LOGGER.severe("Authentication failed because of an internal error: "
                    + ex.getClass().getName());
        }

        if (staff != null) {
            HttpSession session = request.getSession();
            session.setAttribute("user", staff);
            response.sendRedirect("staff-list");
        } else {
            request.setAttribute("error", INVALID_CREDENTIALS_MESSAGE);
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
}
