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
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * A filter used to determine if the current request to the web application
 * should be processed by PALS.
 */
public class PALS_RequestsFilter implements Filter
{
    // Fields - Constants ******************************************************
    /**
     * The name of the attribute which contains the original URL of the request,
     * before being dispatched/redirected.
     */
    public static final String REQUEST_ATTRIBUTE_NAME_ORIGINALURL = "url";
    // Fields ******************************************************************
    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured. 
    private FilterConfig filterConfig = null;
    // Methods - Constructors **************************************************
    public PALS_RequestsFilter() {}
    // Methods *****************************************************************
    /**
     * Invoked during the chain of filter calls; this determines if to delegate
     * work to the servlet handling all requests interfacing with the PALS
     * system.
     * 
     * @param request The servlet request we are processing.
     * @param response The servlet response we are creating.
     * @param chain The filter chain we are processing.
     *
     * @exception IOException if an input/output error occurs.
     * @exception ServletException if a servlet error occurs.
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        // Check the current URL is to be handled by the main servlet
        String url = null;
        if(request instanceof HttpServletRequest && !(url = ((HttpServletRequest)request).getRequestURI()).startsWith("/content"))
        {
            // PALS is to handle the request - forward to servlet!
            request.setAttribute(REQUEST_ATTRIBUTE_NAME_ORIGINALURL, url);
            request.getRequestDispatcher("/pals").forward(request, response);
        }
        else
        {
            // Continue chain of filters/to-servlet
            chain.doFilter(request, response);
        }
    }
    /**
     * Return the filter configuration object for this filter.
     */
    public FilterConfig getFilterConfig()
    {
        return (this.filterConfig);
    }
    /**
     * Set the filter configuration object for this filter.
     *
     * @param filterConfig The filter configuration object.
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
    /**
     * Used to destroy the filter; currently not used.
     */
    public void destroy() {}
    /**
     * Used to initialize the filter.
     */
    public void init(FilterConfig filterConfig)
    {        
        this.filterConfig = filterConfig;
    }
    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString()
    {
        if (filterConfig == null)
            return ("RequestsFilter()");
        StringBuffer sb = new StringBuffer("RequestsFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }
}
