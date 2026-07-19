package fu.swt301.sms.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;

public final class CsrfTokenManager {
    public static final String PARAMETER_NAME = "csrfToken";
    public static final String REQUEST_ATTRIBUTE = "csrfToken";
    private static final String SESSION_ATTRIBUTE = "csrfToken";

    private CsrfTokenManager() {
    }

    public static String ensureToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = null;
        if (session != null) {
            Object existingToken = session.getAttribute(SESSION_ATTRIBUTE);
            if (existingToken instanceof String && !((String) existingToken).isEmpty()) {
                token = (String) existingToken;
            } else {
                token = UUID.randomUUID().toString();
                session.setAttribute(SESSION_ATTRIBUTE, token);
            }
        }

        if (token == null) {
            token = "";
        }
        request.setAttribute(REQUEST_ATTRIBUTE, token);
        return token;
    }

    public static boolean isValid(HttpServletRequest request) {
        String submittedToken = request.getParameter(PARAMETER_NAME);
        if (submittedToken == null || submittedToken.isEmpty()) {
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object expectedToken = session.getAttribute(SESSION_ATTRIBUTE);
        return submittedToken.equals(expectedToken);
    }
}
