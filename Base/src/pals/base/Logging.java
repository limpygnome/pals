package pals.base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import pals.base.utils.DateTime;

/**
 * Used to log the runtime of PALS.
 * 
 * Messages/entries can span across multiple lines, with \EOE\ at the end of an
 * entry.
 * 
 * Thread-safe.
 */
public class Logging 
{
    // Constants ***************************************************************
    public static final String FOLDER = "_logs";
    // Enums *******************************************************************
    public enum EntryType
    {
        /**
         * A log entry which is general information.
         */
        Info,
        /**
         * A log entry which is a warning.
         */
        Warning,
        /**
         * A log entry about a critical error.
         */
        Error
    }
    // Fields ******************************************************************
    private final String    alias;          // The name for the current log-file.
    private PrintWriter     pw;             // The stream to the current log-file.
    private DateTime        logDt;          // The current day of the current log-file, used for deciding if to switch logs when the day changes.
    private boolean         stackTraces;    // Indicates if to log stack-traces of exceptions.
    // Methods - Constructors **************************************************
    private Logging(String alias, boolean stackTraces)
    {
        this.alias = alias;
        this.pw = null;
        this.logDt = DateTime.getInstance();
        this.stackTraces = stackTraces;
    }
    // Methods *****************************************************************
    /**
     * Logs a new exception.
     * 
     * @param ex The exception which has occurred.
     * @param et The log entry type.
     */
    public synchronized void log(Throwable ex, EntryType et)
    {
        log(null, ex, et);
    }
    /**
     * Logs a new exception.
     * 
     * @param message The message to append with the exception, can be null.
     * @param ex The exception which has occurred.
     * @param et The log entry type.
     */
    public synchronized void log(String message, Throwable ex, EntryType et)
    {
        Throwable cause = ex.getCause();
        StringBuilder sb = new StringBuilder();
        // Append message
        if(message != null)
            sb.append(message).append(" - ");
        // Append exception
        sb.append("Exception: '").append(ex.getMessage()).append("'; cause: ");
        if(cause != null)
            sb.append("'").append(cause.getMessage()).append("'");
        else
            sb.append("[unknown].");
        // Append stack-trace (if enabled)
        if(stackTraces)
        {
            StringWriter s = new StringWriter();
            PrintWriter p = new PrintWriter(s);
            ex.printStackTrace(p);
            sb.append(" Stack-trace: '").append(s.toString()).append("'.");
        }
        // Log the message
        log(sb.toString(), et);
    }
    /**
     * Logs a message.
     * @param message The message to be logged.
     * @param et The log entry type.
     */
    public synchronized void log(String message, EntryType et)
    {
        // Verify the message is not null, else ignore...
        if(message == null)
            return;
        // Check if the day has changed
        DateTime dt = DateTime.getInstance();
        if(pw == null || !dt.isSameDay(logDt))
        {
            this.logDt = dt;
            switchLogFile();
        }
        String logEntry;
        {
            StringBuilder sb = new StringBuilder();
            // Write the type of incident
            switch(et)
            {
                case Error:
                    sb.append("ERROR\t"); break;
                case Info:
                    sb.append("INFO\t"); break;
                case Warning:
                    sb.append("WARNING\t"); break;
            }
            // Write the date
            sb.append(String.format("%04d-%02d-%02d %02d:%02d:%02d", dt.getYear(), dt.getMonth(), dt.getDay(), dt.getHour(), dt.getMinute(), dt.getSecond()));
            sb.append("\t");
            // Write the message -- escape EOR end tag too!
            sb.append(message.replace("\\EOE\\", "\\//EOE\\//"));
            sb.append("\\EOE\\");
            logEntry = sb.toString();
        }
        // Output to console and the log-file
        if(et == EntryType.Error)
            System.err.println(logEntry);
        else
            System.out.println(logEntry);
        pw.println(logEntry);
        pw.flush();
    }
    private synchronized boolean switchLogFile()
    {
        // Dispose the current log-file
        if(pw != null)
        {
            pw.flush();
            pw.close();
        }
        try
        {
            // Check the logs folder exists, else create it
            File dir = new File(FOLDER);
            if(!dir.exists())
                dir.mkdir();
            // Build the log path
            // Note: %0x = x padding of digit
            String path = String.format("%s/%s_%04d_%02d_%02d.log", FOLDER, alias, logDt.getYear(), logDt.getMonth(), logDt.getDay());
            // Open new log-file - set to append data too!
            pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(path), true)));
            return true;
        }
        catch(FileNotFoundException ex)
        {
            System.err.println("Failed to create logging instance ~ file exception ~ " + ex.getMessage() + "!");
        }
        catch(IOException ex)   
        {
            System.err.println("Failed to create logging instance ~ IO exception ~ " + ex.getMessage() + "!");
        }
        return false;
    }
    /**
     * Disposes this instance of logging; important to release the file used for
     * logging.
     */
    public synchronized void dispose()
    {
        if(pw != null)
        {
            pw.flush();
            pw.close();
            pw = null;
        }
    }
    // Methods - Static ********************************************************
    /**
     * Creates a new instance of the logger for PALS.
     * 
     * WARNING: only one instance should exist for the specified name, else
     * another logging instance will not be able to access the file.
     * 
     * @param alias The name of the logging file; the date of the current day
     * is also appended.
     * @param stackTraces Indicates if to log stack-traces.
     * 
     * @return Instance or null if an error occurred.
     */
    public static Logging createInstance(String alias, boolean stackTraces)
    {
        Logging l = new Logging(alias, stackTraces);
        if(!l.switchLogFile())
        {
            l.dispose();
            return null;
        }
        else
            return l;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param enabled Sets if stack-traces are logged with exceptions.
     */
    public void setStackTraces(boolean enabled)
    {
        this.stackTraces = enabled;
    }
}
