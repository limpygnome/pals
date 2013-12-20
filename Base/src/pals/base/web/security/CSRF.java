package pals.base.web.security;

import pals.base.utils.Misc;
import pals.base.web.WebRequestData;

/**
 * A class for protecting against cross-site request forgery (CSRF). For more
 * information, refer to:
 * https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)
 * 
 */
public class CSRF
{
    // Constants ***********************************************************
    private static final int CSRF_TEXT_LENGTH = 32;
    private static final String CSRF_SESSION_KEY = "csrf_token";
    // Methods - Static ****************************************************
    /**
     * Generates and returns a CSRF token. This token is stored in the
     * user's session for confirmation later.
     * 
     * @param data The data for the current web-request.
     * @return The token generated; this should be embedded in a form and
     * sent back to csrfSecure method to later confirm a request,
     * such as a postback from a form, is secure.
     */
    public static String set(WebRequestData data)
    {
        String token = Misc.randomText(data.getCore(), CSRF_TEXT_LENGTH);
        data.getSession().setAttribute(CSRF_SESSION_KEY, token);
        return token;
    }
    /**
     * Validates a token, provided by a user, is the same as the token
     * generated earlier.
     * 
     * @param data The data for the current web-request.
     * @param userInput The token provided by the user for the current
     * request.
     * @return True = secure/valid, false = insecure/invalid.
     */
    public static boolean isSecure(WebRequestData data, String userInput)
    {
        if(userInput == null || userInput.length() != CSRF_TEXT_LENGTH)
            return false;
        String v = data.getSession().getAttribute(CSRF_SESSION_KEY);
        return v != null && v.length() == CSRF_TEXT_LENGTH && v.equals(userInput);
    }
}
