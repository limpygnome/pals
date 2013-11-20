import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import pals.base.RMI_Interface;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;

/**
 * The servlet for handling web-requests to the PALS system.
 */
@WebServlet(urlPatterns = {"/pals"})
public class RequestHandlerServlet extends HttpServlet
{
    private enum ResponseType
    {
        Error_RMI,
        Error_NoOutput,
        Success
    }
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        ResponseType rt = ResponseType.Success;
        try
        {
            // Prepare remote request wrapper
            RemoteRequest dataRequest = new RemoteRequest();
            // Communicate to node using RMI
            Registry r = LocateRegistry.getRegistry(1099);
            RMI_Interface ri = (RMI_Interface)r.lookup(RMI_Interface.class.getName());
            RemoteResponse dataResponse = ri.handleWebRequest(dataRequest);
            // Handle response data
            {
                byte[] buffer = dataResponse.getBuffer();
                if(buffer != null && buffer.length != 0)
                {
                    ServletOutputStream sos = response.getOutputStream();
                    sos.write(buffer);
                    sos.flush();
                }
                else
                    rt = ResponseType.Error_NoOutput;
            }
        }
        catch(RemoteException ex)
        {
            System.err.println("RMI RemoteException ~ " + ex.getMessage() + "!");
            rt = ResponseType.Error_RMI;
        }
        catch(NotBoundException ex)
        {
            System.err.println("RMI NotBoundException ~ " + ex.getMessage() + "!");
            rt = ResponseType.Error_RMI;
        }
        // Check if we have handled the response correctly, else output a message to the user
        if(rt == ResponseType.Success)
            return;
        // An error has occurred...
        PrintWriter pw = response.getWriter();
        pw.println("<!DOCTYPE html><html><head><title>PALS - Communication Issue</title></head><body>");
        pw.println("<h1>Error</h1>");
        switch(rt)
        {
            case Error_RMI:
                pw.println("<p>Communication issue, please try again...</p>");
                pw.println("<h2>Network Administrators</h2>");
                pw.println("<p>The system is unable to communicate with the node process, check it is running!</p>");
                break;
            case Error_NoOutput:
                pw.println("<p>Communication issue, please try again...</p>");
                pw.println("<h2>Network Administrators</h2>");
                pw.println("<p>No data for the web-page was returned from the node; check templates and plugins are loading correctly.</p>");
                break;
        }
        pw.println("</body></html>");
        pw.flush();
        pw.close();
    }
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request.
     * @param response servlet response.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request.
     * @param response servlet response.
     * @throws ServletException if a servlet-specific error occurs.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "The servlet responsible for handling requests to the PALS system.";
    }
    // </editor-fold>
}
