import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import pals.base.RMI_Interface;
import pals.base.Settings;
import pals.base.SettingsException;
import pals.base.Storage;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import pals.base.web.UploadedFile;

/**
 * The servlet for handling web-requests to the PALS system.
 */
public class PALS_Servlet extends HttpServlet
{
    // Enums *******************************************************************
    private enum ResponseType
    {
        Error_Settings,
        Error_RMI,
        Error_NoOutput,
        Success
    }
    // Fields - Constants ******************************************************
    /**
     * The name of the cookie storing the session identifier.
     */
    public static final String SESSION_COOKIE_NAME = "pals_sessid";
    // Methods *****************************************************************
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
        ResponseType rt = ResponseType.Success;
        try
        {
            // Check the settings have been loaded
            Settings settings;
            if((settings = PALS_SettingsListener.getSettings()) == null)
                throw new SettingsException(SettingsException.Type.FailedToLoad, null);
            // Build request
            // -- Fetch session identifier
            String sessid = getCookie(request, SESSION_COOKIE_NAME);
            // -- Build relative url of request
            String relUrl = (String)request.getAttribute(PALS_RequestsFilter.REQUEST_ATTRIBUTE_NAME_ORIGINALURL);
            // -- Prepare remote request wrapper
            RemoteRequest dataRequest = new RemoteRequest(sessid, relUrl, request.getRemoteAddr());
            // -- Add request fields
            for(Map.Entry<String,String[]> param : request.getParameterMap().entrySet())
            {
                dataRequest.setFields(param.getKey(), param.getValue());
            }
            // -- Add request files (and possibly fields)
            if(ServletFileUpload.isMultipartContent(request))
            {
                Random rng = new Random(System.currentTimeMillis());
                ServletFileUpload uploads = new ServletFileUpload();
                try
                {
                    // Prepare temp storage dir
                    String tempFolder = Storage.getPath_tempWeb(settings.getStr("storage/path"));
                    {
                        File tf = new File(tempFolder);
                        if(!tf.exists())
                            tf.mkdir();
                    }
                    FileItemIterator itFile = uploads.getItemIterator(request);
                    // Iterate each upload item
                    String filename;
                    FileItemStream fis;
                    FileOutputStream fos;
                    InputStream is;
                    long size;
                    int bytesRead;
                    byte[] data;
                    while(itFile.hasNext())
                    {
                        fis = itFile.next();
                        is = fis.openStream();
                        if(!fis.isFormField())
                        {
                            // Build filename
                            filename = "part_" + request.getRemoteAddr().replace(":", ".") + "_" + rng.nextInt() + ".part";
                            // Write data to disk
                            size = 0;
                            fos = new FileOutputStream(new File(tempFolder + "/" + filename));
                            data = new byte[1024];
                            while((bytesRead = is.read(data)) != -1)
                            {
                                fos.write(data, 0, bytesRead);
                                size += bytesRead;
                            }
                            fos.flush();
                            fos.close();
                            // Add to request
                            dataRequest.setFile(fis.getFieldName(), new UploadedFile(fis.getName(), fis.getContentType(), size, filename));
                        }
                        else
                        {
                            // Not a file - a field - set anyhow!
                            dataRequest.setField(fis.getFieldName(), Streams.asString(is));
                        }
                    }
                }
                catch(FileUploadException ex)
                {
                    System.err.println("Failed to receive upload from user '" + request.getRemoteAddr() + "' ~ " + ex.getMessage());
                }
            }

            // Communicate to node using RMI
            Registry r = LocateRegistry.getRegistry(settings.getStr("rmi/ip"), settings.getInt("rmi/port"));
            RMI_Interface ri = (RMI_Interface)r.lookup(RMI_Interface.class.getName());
            RemoteResponse dataResponse = ri.handleWebRequest(dataRequest);
            
            // Handle response
            // -- Update the session ID
            if(!dataResponse.getSessionID().equals(sessid))
            {
                Cookie cookieSess = new Cookie(SESSION_COOKIE_NAME, dataResponse.getSessionID());
                cookieSess.setPath("/");
                cookieSess.setMaxAge(dataResponse.isSessionPrivate() ? 3600 : 600);
                response.addCookie(cookieSess);
            }
            // -- Check for redirect
            String redirect = dataResponse.getRedirectUrl();
            if(redirect != null)
            {
                if(!redirect.startsWith("/"))
                    redirect = "/" + redirect;
                response.sendRedirect(request.getContextPath() + redirect);
            }
            else
            {
                // -- Handle response type
                response.setContentType(dataResponse.getResponseType());
                // -- Set response code
                response.setStatus(dataResponse.getResponseCode());
                // -- Handle response data
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
            // Note: nothing else can be sent now; thus do not set any
            // cookies or headers at this point.
            
            // Destroy any temp files
            String tempFolder = Storage.getPath_tempWeb(settings.getStr("storage/path"));
            File file;
            for(UploadedFile uf : dataRequest.getFiles())
            {
                file = new File(tempFolder + "/" + uf.getTempName());
                if(file.exists() && file.isFile())
                    file.delete();
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
        catch(SettingsException ex)
        {
            System.err.println("Settings exception ~ " + ex.getMessage() + "!");
            rt = ResponseType.Error_Settings;
        }
        // Check if we have handled the response correctly, else output a message to the user
        if(rt == ResponseType.Success)
            return;
        // An error has occurred...
        response.setContentType("text/html;charset=UTF-8");
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
            case Error_Settings:
                pw.println("<p>Error occurred reading settings...</p>");
                pw.println("<h2>Network Administrators</h2>");
                pw.println("<p>Ensure the file at WEB-INF/web.config as been correctly configured.</p>");
                break;
        }
        pw.println("</body></html>");
        pw.flush();
        pw.close();
    }
    private String getCookie(HttpServletRequest request, String name)
    {
        Cookie[] cookies = request.getCookies();
        if(cookies != null)
        {
            for(int i = 0; i < cookies.length; i++)
            {
                if(cookies[i].getName().equals(name))
                    return cookies[i].getValue();
            }
        }
        return null;
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
