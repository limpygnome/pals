
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
public class RequestsFilter implements Filter
{
    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured. 
    private FilterConfig filterConfig = null;
    
    public RequestsFilter() {}

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
            request.getRequestDispatcher("/pals").forward(request, response);
        }
        // Continue chain of filters/to-servlet
        chain.doFilter(request, response);
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
        if (filterConfig == null) {
            return ("RequestsFilter()");
        }
        StringBuffer sb = new StringBuffer("RequestsFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }
}
