/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.web.security;

import pals.base.utils.Misc;
import pals.base.web.WebRequestData;

/**
 * A class for protecting against cross-site request forgery (CSRF). For more
 * information, refer to:
 * https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)
 * 
 * @version 1.0
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
     * @since 1.0
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
     * The request should have a field called "csrf"; this is
     * null-protected.
     * 
     * @param data The data for the current web-request.
     * @return True = secure/valid, false = insecure/invalid.
     * @since 1.0
     */
    public static boolean isSecure(WebRequestData data)
    {
        return isSecure(data, data.getRequestData().getField("csrf"));
    }
    /**
     * Validates a token, provided by a user, is the same as the token
     * generated earlier.
     * 
     * @param data The data for the current web-request.
     * @param userInput The token provided by the user for the current
     * request.
     * @return True = secure/valid, false = insecure/invalid.
     * @since 1.0
     */
    public static boolean isSecure(WebRequestData data, String userInput)
    {
        if(userInput == null || userInput.length() != CSRF_TEXT_LENGTH)
            return false;
        String v = data.getSession().getAttribute(CSRF_SESSION_KEY);
        return v != null && v.length() == CSRF_TEXT_LENGTH && v.equals(userInput);
    }
}
